/* Copyright 2006-2010 the original author or authors.
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
package org.grails.plugins.springsecurity.service.acl

import org.apache.log4j.Level
import org.apache.log4j.Logger

import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclClass
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

import org.springframework.security.acls.domain.AccessControlEntryImpl
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.jdbc.JdbcAclService;
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AlreadyExistsException
import org.springframework.security.acls.model.AuditableAccessControlEntry
import org.springframework.security.acls.model.ChildrenExistException
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

/**
 * GORM implementation of {@link org.springframework.security.acls.model.AclService} and {@link MutableAclService}.
 * Ported from <code>JdbcAclService</code> and <code>JdbcMutableAclService</code>.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclService implements MutableAclService {

	private final Logger log = Logger.getLogger(getClass())

	// individual methods are @Transactional since NotFoundException
	// is a runtime exception and will cause an unwanted transaction rollback
	static transactional = false

	/** Dependency injection for aclLookupStrategy. */
	def aclLookupStrategy

	/** Dependency injection for aclCache. */
	def aclCache

	/** Dependency injection for messageSource. */
	def messageSource

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.MutableAclService#createAcl(
	 * 	org.springframework.security.acls.model.ObjectIdentity)
	 */
	@Transactional
	MutableAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
		Assert.notNull objectIdentity, 'Object Identity required'

		// Check this object identity hasn't already been persisted
		if (retrieveObjectIdentity(objectIdentity)) {
			throw new AlreadyExistsException("Object identity '$objectIdentity' already exists")
		}

		// Need to retrieve the current principal, in order to know who "owns" this ACL (can be changed later on)
		PrincipalSid sid = new PrincipalSid(SCH.context.authentication)

		// Create the acl_object_identity row
		createObjectIdentity objectIdentity, sid

		return readAclById(objectIdentity)
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
		return aclSid
	}

	protected AclClass createOrRetrieveClass(String className, boolean allowCreate) {
		AclClass aclClass = AclClass.findByClassName(className)
		if (!aclClass && allowCreate) {
			aclClass = save(new AclClass(className: className))
		}
		return aclClass
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.MutableAclService#deleteAcl(
	 * 	org.springframework.security.acls.model.ObjectIdentity, boolean)
	 */
	@Transactional
	void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren) throws ChildrenExistException {

		Assert.notNull objectIdentity, 'Object Identity required'
		Assert.notNull objectIdentity.identifier, "Object Identity doesn't provide an identifier"

		if (deleteChildren) {
			for (child in findChildren(objectIdentity)) {
				deleteAcl child, true
			}
		}

		AclObjectIdentity oid = retrieveObjectIdentity(objectIdentity)

		// Delete this ACL's ACEs in the acl_entry table
		deleteEntries oid

		// Delete this ACL's acl_object_identity row
		oid.delete()

		// Clear the cache
		aclCache.evictFromCache objectIdentity
	}

	protected void deleteEntries(AclObjectIdentity oid) {
		AclEntry.executeUpdate(
				"DELETE FROM AclEntry ae " +
				"WHERE ae.aclObjectIdentity = :oid", [oid: oid])
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.MutableAclService#updateAcl(
	 * 	org.springframework.security.acls.model.MutableAcl)
	 */
	@Transactional
	MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
		Assert.notNull acl.id, "Object Identity doesn't provide an identifier"

		// Delete this ACL's ACEs in the acl_entry table
		deleteEntries retrieveObjectIdentity(acl.objectIdentity)

		// Create this ACL's ACEs in the acl_entry table
		createEntries acl

		// Change the mutable columns in acl_object_identity
		updateObjectIdentity acl

		// Clear the cache, including children
		clearCacheIncludingChildren acl.objectIdentity

		return readAclById(acl.objectIdentity)
	}

	protected void createEntries(MutableAcl acl) {
		int i = 0
		for (AuditableAccessControlEntry entry in acl.entries) {
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
		aclCache.evictFromCache(objectIdentity)
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#findChildren(
	 * 	org.springframework.security.acls.model.ObjectIdentity)
	 */
	List<ObjectIdentity> findChildren(ObjectIdentity parent) {
		def children = AclObjectIdentity.executeQuery(
				"FROM AclObjectIdentity " +
				"WHERE parent.objectId = :objectId " +
				"  AND parent.aclClass.className = :className",
				[objectId: parent.identifier,
				 className: parent.type])

		if (!children) {
			return null
		}

		def results = []
		for (AclObjectIdentity aclObjectIdentity in children) {
			results << new ObjectIdentityImpl(
					lookupClass(aclObjectIdentity.aclClass.className), aclObjectIdentity.objectId)
		}
		results
	}

	protected Class<?> lookupClass(String className) {
		// workaround for Class.forName() not working in tests
		return Class.forName(className, true, Thread.currentThread().contextClassLoader)
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclById(
	 * 	org.springframework.security.acls.model.ObjectIdentity)
	 */
	Acl readAclById(ObjectIdentity object) throws NotFoundException {
		readAclById object, null
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclById(
	 * 	org.springframework.security.acls.model.ObjectIdentity, java.util.List)
	 */
	Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
		Map<ObjectIdentity, Acl> map = readAclsById([object], sids)
		Assert.isTrue map.containsKey(object),
				"There should have been an Acl entry for ObjectIdentity $object"
		map[object]
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclsById(java.util.List)
	 */
	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
		readAclsById objects, null
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.AclService#readAclsById(
	 * 	java.util.List, java.util.List)
	 */
	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) throws NotFoundException {
		Map<ObjectIdentity, Acl> result = aclLookupStrategy.readAclsById(objects, sids)
		// Check every requested object identity was found (throw NotFoundException if needed)
		for (ObjectIdentity object in objects) {
			if (!result.containsKey(object)) {
				throw new NotFoundException(
						"Unable to find ACL information for object identity '$object'")
			}
		}
		return result
	}

	protected AclObjectIdentity retrieveObjectIdentity(ObjectIdentity oid) {
		return AclObjectIdentity.executeQuery(
				"FROM AclObjectIdentity " +
				"WHERE aclClass.className = :className " +
				"  AND objectId = :objectId",
				[className: oid.type,
				 objectId: oid.identifier])[0]
	}

	protected save(bean) {
		bean.validate()
		if (bean.hasErrors()) {
			if (log.isEnabledFor(Level.WARN)) {
				def message = new StringBuilder(
						"problem creating ${bean.getClass().simpleName}: $bean")
				def locale = Locale.getDefault()
				for (fieldErrors in bean.errors) {
					for (error in fieldErrors.allErrors) {
						message.append('\n\t')
						message.append(messageSource.getMessage(error, locale))
					}
				}
				log.warn message
			}
		}
		else {
			bean.save()
		}

		bean
	}
}
