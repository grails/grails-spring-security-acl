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
package org.codehaus.groovy.grails.plugins.springsecurity.acl

import org.springframework.security.acls.domain.AccessControlEntryImpl
import org.springframework.security.acls.domain.AclImpl
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.jdbc.LookupStrategy
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Permission
import org.springframework.security.acls.model.Sid
import org.springframework.util.Assert

/**
 * GORM implementation of {@link LookupStrategy}. Ported from <code>BasicLookupStrategy</code>.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GormAclLookupStrategy implements LookupStrategy {

	/** Dependency injection for aclAuthorizationStrategy. */
	def aclAuthorizationStrategy

	/** Dependency injection for aclCache. */
	def aclCache

	/** Dependency injection for auditLogger. */
	def auditLogger

	/** Dependency injection for permissionFactory. */
	def permissionFactory

	int batchSize = 50

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.jdbc.LookupStrategy#readAclsById(
	 * 	java.util.List, java.util.List)
	 */
	Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) {
		Map<ObjectIdentity, Acl> result = [:]
		Set<ObjectIdentity> currentBatchToLoad = []

		objects.eachWithIndex { object, i ->
			// Check we don't already have this ACL in the results
			boolean aclFound = result.containsKey(object)

			// Check cache for the present ACL entry
			if (!aclFound) {
				Acl acl = aclCache.getFromCache(object)

				// Ensure any cached element supports all the requested SIDs
				// (they should always, as our base impl doesn't filter on SID)
				if (acl) {
					if (acl.isSidLoaded(sids)) {
						result[acl.objectIdentity] = acl
						aclFound = true
					}
					else {
						throw new IllegalStateException(
								'Error: SID-filtered element detected when implementation does not perform SID filtering ' +
								'- have you added something to the cache manually?')
					}
				}
			}

			// Load the ACL from the database
			if (!aclFound) {
				currentBatchToLoad << object
			}

			// Is it time to load from JDBC the currentBatchToLoad?
			if (currentBatchToLoad.size() == batchSize || (i + 1) == objects.size()) {
				if (currentBatchToLoad.size() > 0) {
					Map<ObjectIdentity, Acl> loadedBatch = lookupObjectIdentities(
							currentBatchToLoad, sids)
					// Add loaded batch (all elements 100% initialized) to results
					result.putAll loadedBatch
					// Add the loaded batch to the cache
					for (acl in loadedBatch.values()) {
						aclCache.putInCache acl
					}
					currentBatchToLoad.clear()
				}
			}
		}

		return result
	}

	protected Map<ObjectIdentity, Acl> lookupObjectIdentities(
			Collection<ObjectIdentity> objectIdentities, List<Sid> sids) {

		Assert.notEmpty objectIdentities, 'Must provide identities to lookup'

		Map<Serializable, Acl> acls = [:] // contains Acls with StubAclParents

		def hql = new StringBuilder("FROM $AclObjectIdentity.name WHERE 1=0 ")
		def params = [:]
		for (ObjectIdentity objectIdentity in objectIdentities) {
			hql.append ' OR (objectId = :objectId AND aclClass.className = :className) '
			params.objectId = objectIdentity.identifier
			params.className = objectIdentity.type
		}
		hql.append ' ORDER BY objectId ASC'

		def aclObjectIdentities = AclObjectIdentity.executeQuery(hql.toString(), params)
		Map<AclObjectIdentity, List<AclEntry>> aclObjectIdentityMap = findAcls(aclObjectIdentities)

		List<AclObjectIdentity> parents = convertEntries(aclObjectIdentityMap, acls, sids)
		if (parents) {
			lookupParents acls, parents, sids
		}

		// Finally, convert our 'acls' containing StubAclParents into true Acls
		Map<ObjectIdentity, Acl> result = [:]
		for (Acl inputAcl in acls.values()) {
			Acl converted = convert(acls, inputAcl.id)
			result[converted.objectIdentity] = converted
		}

		return result
	}

	protected Map<AclObjectIdentity, List<AclEntry>> findAcls(
			List<AclObjectIdentity> aclObjectIdentities) {

		List<AclEntry> entries
		if (aclObjectIdentities) {
			entries = AclEntry.executeQuery(
					"FROM $AclEntry.name " +
					"WHERE aclObjectIdentity IN (:aclObjectIdentities) " +
					"ORDER BY aceOrder ASC",
					[aclObjectIdentities: aclObjectIdentities])
		}

		def map = [:]
		for (AclObjectIdentity aclObjectIdentity in aclObjectIdentities) {
			map[aclObjectIdentity] = []
		}

		for (entry in entries) {
			map[entry.aclObjectIdentity] << entry
		}

		return map
	}

	protected AclImpl convert(Map<Serializable, Acl> inputMap, Serializable currentIdentity) {
		Assert.notEmpty inputMap, 'InputMap required'
		Assert.notNull currentIdentity, 'CurrentIdentity required'

		// Retrieve this Acl from the InputMap
		Acl inputAcl = inputMap[currentIdentity]
		Assert.isInstanceOf AclImpl, inputAcl, 'The inputMap contained a non-AclImpl'

		Acl parent = inputAcl.parentAcl
		if (parent instanceof StubAclParent) {
			parent = convert(inputMap, parent.id)
		}

		// Now we have the parent (if there is one), create the true AclImpl
		AclImpl result = new AclImpl(inputAcl.objectIdentity, inputAcl.id,
				aclAuthorizationStrategy, auditLogger, parent, null,
				inputAcl.isEntriesInheriting(), inputAcl.owner)

		List acesNew = []
		for (AccessControlEntryImpl ace in inputAcl.@aces) {
			ace.@acl = result
			acesNew << ace
		}
		result.@aces = acesNew

		return result
	}

	protected List<AclObjectIdentity> convertEntries(
			Map<AclObjectIdentity, List<AclEntry>> aclObjectIdentityMap,
			Map<Serializable, Acl> acls, List<Sid> sids) {

		List<AclObjectIdentity> parents = []

      aclObjectIdentityMap.each { aclObjectIdentity, aclEntries ->
   		createAcl acls, aclObjectIdentity, aclEntries

			if (aclObjectIdentity.parent) {
				Serializable parentId = aclObjectIdentity.parent.id
				if (acls.containsKey(parentId)) {
					return
				}

				// Now try to find it in the cache
				MutableAcl cached = aclCache.getFromCache(parentId)
				if (!cached || !cached.isSidLoaded(sids)) {
					parents << aclObjectIdentity.parent
				}
				else {
					// Pop into the acls map, so our convert method doesn't
					// need to deal with an unsynchronized AclCache
					acls[cached.id] = cached
				}
			}
		}

		return parents
	}

	protected void createAcl(Map<Serializable, Acl> acls, AclObjectIdentity aclObjectIdentity,
			List<AclEntry> entries) {

		Serializable id = aclObjectIdentity.id

		// If we already have an ACL for this ID, just create the ACE
		AclImpl acl = acls[id]
		if (!acl) {
			// Make an AclImpl and pop it into the Map
			def objectIdentity = new ObjectIdentityImpl(
					lookupClass(aclObjectIdentity.aclClass.className),
					aclObjectIdentity.objectId)
			Acl parentAcl
			if (aclObjectIdentity.parent) {
				parentAcl = new StubAclParent(aclObjectIdentity.parent.id)
			}

			def ownerSid = aclObjectIdentity.owner
			Sid owner = ownerSid.principal ?
					new PrincipalSid(ownerSid.sid) :
					new GrantedAuthoritySid(ownerSid.sid)

			acl = new AclImpl(objectIdentity, id, aclAuthorizationStrategy, auditLogger,
					parentAcl, null, aclObjectIdentity.entriesInheriting, owner)
			acls[id] = acl
		}

		List aces = acl.@aces
		for (AclEntry entry in entries) {
			// Add an extra ACE to the ACL (ORDER BY maintains the ACE list order)
			// It is permissable to have no ACEs in an ACL
			String aceSid = entry.sid?.sid
			if (aceSid) {
				Sid recipient = entry.sid.principal ?
						new PrincipalSid(aceSid) :
						new GrantedAuthoritySid(aceSid)

				Permission permission = permissionFactory.buildFromMask(entry.mask)
				AccessControlEntryImpl ace = new AccessControlEntryImpl(
						entry.id, acl, recipient, permission,
						entry.granting, entry.auditSuccess, entry.auditFailure)

				// Add the ACE if it doesn't already exist in the ACL.aces field
				if (!aces.contains(ace)) {
					aces << ace
				}
			}
		}
	}

	protected Class<?> lookupClass(String className) {
		// workaround for Class.forName() not working in tests
		return Class.forName(className, true, Thread.currentThread().contextClassLoader)
	}

	protected void lookupParents(Map<Serializable, Acl> acls,
			Collection<AclObjectIdentity> findNow, List<Sid> sids) {

		Assert.notNull acls, 'ACLs are required'
		Assert.notEmpty findNow, 'Items to find now required'

		Map<AclObjectIdentity, List<AclEntry>> aclObjectIdentityMap = findAcls(findNow as List)
		List<AclObjectIdentity> parents = convertEntries(aclObjectIdentityMap, acls, sids)
		if (parents) {
			lookupParents acls, parents, sids
		}
	}
}
