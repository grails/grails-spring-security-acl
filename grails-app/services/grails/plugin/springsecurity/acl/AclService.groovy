/* Copyright 2009-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.springsecurity.acl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.security.acls.domain.AccessControlEntryImpl
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.jdbc.LookupStrategy
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AclCache
import org.springframework.security.acls.model.AlreadyExistsException
import org.springframework.security.acls.model.AuditableAccessControlEntry
import org.springframework.security.acls.model.ChildrenExistException
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

/**
 * GORM implementation of {@link org.springframework.security.acls.model.AclService} and {@link MutableAclService}.
 * Ported from <code>JdbcAclService</code> and <code>JdbcMutableAclService</code>.
 *
 * Individual methods are @Transactional since NotFoundException
 * is a runtime exception and will cause an unwanted transaction rollback
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclService implements MutableAclService {

	protected final Logger log = LoggerFactory.getLogger(getClass())

	/** Dependency injection for aclLookupStrategy. */
	LookupStrategy aclLookupStrategy

	/** Dependency injection for aclCache. */
	AclCache aclCache

	/** Dependency injection for messageSource. */
	MessageSource messageSource

	@Transactional
	MutableAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
		Assert.notNull objectIdentity, 'Object Identity required'

		// Check this object identity hasn't already been persisted
		if (retrieveObjectIdentity(objectIdentity)) {
			throw new AlreadyExistsException("Object identity '$objectIdentity' already exists")
		}

		// Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
		PrincipalSid sid = new PrincipalSid(SecurityContextHolder.context.authentication)

		// Create the acl_object_identity row
		createObjectIdentity objectIdentity, sid

		readAclById objectIdentity
	}

	protected void createObjectIdentity(ObjectIdentity object, Sid owner) {
		AclSid ownerSid = createOrRetrieveSid(owner, true)
		AclClass aclClass = createOrRetrieveClass(object.type, true)
		save new AclObjectIdentity(
				aclClass: aclClass,
				objectId: object.identifier,
				owner: ownerSid,
				entriesInheriting: true)
	}

	protected AclSid createOrRetrieveSid(Sid sid, boolean allowCreate) {
		Assert.notNull sid, 'Sid required'

		String sidName
		boolean principal
		if (sid instanceof PrincipalSid) {
			sidName = sid.principal
			principal = true
		}
		else if (sid instanceof GrantedAuthoritySid) {
			sidName = sid.grantedAuthority
			principal = false
		}
		else {
			throw new IllegalArgumentException('Unsupported implementation of Sid')
		}

		AclSid aclSid = AclSid.findBySidAndPrincipal(sidName, principal)
		if (!aclSid && allowCreate) {
			aclSid = save(new AclSid(sid: sidName, principal: principal))
		}
		aclSid
	}

	protected AclClass createOrRetrieveClass(String className, boolean allowCreate) {
		AclClass aclClass = AclClass.findByClassName(className)
		if (!aclClass && allowCreate) {
			aclClass = save(new AclClass(className: className))
		}
		aclClass
	}

	@Transactional
	void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren) throws ChildrenExistException {

		Assert.notNull objectIdentity, 'Object Identity required'
		Assert.notNull objectIdentity.identifier, "Object Identity doesn't provide an identifier"

		if (deleteChildren) {
			findChildren(objectIdentity).each { deleteAcl it, true }
		}

		AclObjectIdentity oid = retrieveObjectIdentity(objectIdentity)
		if (oid) {
			// Delete this ACL's ACEs in the acl_entry table
			deleteEntries oid

			// Delete this ACL's acl_object_identity row
			oid.delete()
		}

		// Clear the cache
		aclCache.evictFromCache objectIdentity
	}

	protected void deleteEntries(AclObjectIdentity oid) {
		if (oid) {
			AclEntry.where { aclObjectIdentity == oid }.deleteAll()
		}
	}

	protected void deleteEntries(List<AclEntry> entries) {
		log.debug 'Deleting entries: {}', entries
		entries*.delete()
	}

	@Transactional
	MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
		Assert.notNull acl.id, "Object Identity doesn't provide an identifier"

		def aclObjectIdentity = retrieveObjectIdentity(acl.objectIdentity)

		List<AclEntry> existingAces = AclEntry.findAllByAclObjectIdentity(aclObjectIdentity)

		List<AclEntry> toDelete = existingAces.findAll { AclEntry ace ->
			log.trace 'Checking ace for delete: {}', ace
			!acl.entries.find { AccessControlEntry entry ->
				log.trace 'Checking entry for delete: {}', entry
				entry.permission.mask == ace.mask && entry.sid == ace.sid.sid
			}
		}

		List<AccessControlEntry> toCreate = acl.entries.findAll { AccessControlEntry entry ->
			log.trace 'Checking entry for create: {}', entry
			!existingAces.find { AclEntry ace ->
				log.trace 'Checking ace for create: {}', ace
				entry.permission.mask == ace.mask && entry.sid == ace.sid.sid
			}
		}

		// Delete this ACL's ACEs in the acl_entry table
		deleteEntries toDelete

		// Create this ACL's ACEs in the acl_entry table
		createEntries acl, toCreate

		// Change the mutable columns in acl_object_identity
		updateObjectIdentity acl

		// Clear the cache, including children
		clearCacheIncludingChildren acl.objectIdentity

		readAclById acl.objectIdentity
	}

	protected void createEntries(MutableAcl acl, List<AuditableAccessControlEntry> entries = null) {
		entries = entries ?: acl.entries
		int i = 0
		for (AuditableAccessControlEntry entry in entries) {
			Assert.isInstanceOf AccessControlEntryImpl, entry, 'Unknown ACE class'
			save new AclEntry(
					aclObjectIdentity: AclObjectIdentity.get(acl.id),
					aceOrder: i++,
					sid: createOrRetrieveSid(entry.sid, true),
					mask: entry.permission.mask,
					granting: entry.isGranting(),
					auditSuccess: entry.isAuditSuccess(),
					auditFailure: entry.isAuditFailure())
		}
	}

	protected void updateObjectIdentity(MutableAcl acl) {
		Assert.notNull acl.owner, "Owner is required in this implementation"

		AclObjectIdentity aclObjectIdentity = AclObjectIdentity.get(acl.id)

		AclObjectIdentity parent
		if (acl.parentAcl) {
			def oii = acl.parentAcl.objectIdentity
			Assert.isInstanceOf ObjectIdentityImpl, oii,
				'Implementation only supports ObjectIdentityImpl'
			parent = retrieveObjectIdentity(oii)
		}

		aclObjectIdentity.parent = parent
		aclObjectIdentity.owner = createOrRetrieveSid(acl.owner, true)
		aclObjectIdentity.entriesInheriting = acl.isEntriesInheriting()
	}

	protected void clearCacheIncludingChildren(ObjectIdentity objectIdentity) {
		Assert.notNull objectIdentity, 'ObjectIdentity required'
		for (child in findChildren(objectIdentity)) {
			clearCacheIncludingChildren child
		}
		aclCache.evictFromCache objectIdentity
	}

	List<ObjectIdentity> findChildren(ObjectIdentity parentOid) {
		def children = AclObjectIdentity.withCriteria {
			parent {
				eq 'objectId', parentOid.identifier
				aclClass {
					eq 'className', parentOid.type
				}
			}
		}

		if (!children) {
			return null
		}

		children.collect { AclObjectIdentity aoi -> new ObjectIdentityImpl(lookupClass(aoi.aclClass.className), aoi.objectId) }
	}

	protected Class<?> lookupClass(String className) {
		// workaround for Class.forName() not working in tests
		Class.forName className, true, Thread.currentThread().contextClassLoader
	}

	Acl readAclById(ObjectIdentity object) throws NotFoundException {
		readAclById object, null
	}

	Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
		Map<ObjectIdentity, Acl> map = readAclsById([object], sids)
		Assert.isTrue map.containsKey(object),
				"There should have been an Acl entry for ObjectIdentity $object"
		map[object]
	}

	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
		readAclsById objects, null
	}

	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) throws NotFoundException {
		Map<ObjectIdentity, Acl> result = aclLookupStrategy.readAclsById(objects, sids)
		// Check every requested object identity was found (throw NotFoundException if needed)
		for (ObjectIdentity object in objects) {
			if (!result.containsKey(object)) {
				throw new NotFoundException("Unable to find ACL information for object identity '$object'")
			}
		}
		return result
	}

	protected AclObjectIdentity retrieveObjectIdentity(ObjectIdentity oid) {
		AclObjectIdentity.withCriteria {
			eq 'objectId', oid.identifier
			aclClass {
				eq 'className', oid.type
			}
			maxResults 1
		}[0]
	}

	protected save(bean) {
		if (!bean.save()) {
			if (log.warnEnabled) {
				def message = new StringBuilder("problem creating ${bean.getClass().simpleName}: $bean")
				def locale = Locale.getDefault()
				for (fieldErrors in bean.errors) {
					for (error in fieldErrors.allErrors) {
						message << '\n\t' << messageSource.getMessage(error, locale)
					}
				}
				log.warn message
			}
		}

		bean
	}
}
