/* Copyright 2009-2012 SpringSource.
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

import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclClass
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.CumulativePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AlreadyExistsException
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Permission
import org.springframework.security.acls.model.Sid
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.context.SecurityContextHolder as SCH

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclServiceTests extends GroovyTestCase {

	private final Authentication auth = new TestingAuthenticationToken(
			'ben', 'ignored', new GrantedAuthorityImpl('ROLE_ADMIN'))

	private final ObjectIdentity topParentOid = new ObjectIdentityImpl(Report, 100L)
	private final ObjectIdentity middleParentOid = new ObjectIdentityImpl(Report, 101L)
	private final ObjectIdentity childOid = new ObjectIdentityImpl(Report, 102L)

	private final List<Permission> read = [BasePermission.READ]
	private final List<Permission> write = [BasePermission.WRITE]
	private final List<Permission> delete = [BasePermission.DELETE]
	private final List<Permission> administration = [BasePermission.ADMINISTRATION]
	private final List<Permission> create = [BasePermission.CREATE]

	private PrincipalSid principalSid

   def aclCache
  	def aclService
	def ehcacheAclCache
	def sessionFactory

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()
		auth.authenticated = true
		principalSid = new PrincipalSid(auth)
		SCH.context.authentication = auth
	}

	void testLifecycle() {

		MutableAcl topParent = aclService.createAcl(topParentOid)
		MutableAcl middleParent = aclService.createAcl(middleParentOid)
		MutableAcl child = aclService.createAcl(childOid)

		// Specify the inheritance hierarchy
		middleParent.parent = topParent
		child.parent = middleParent

		// Now let's add a couple of permissions
		topParent.insertAce 0, BasePermission.READ, principalSid, true
		topParent.insertAce 1, BasePermission.WRITE, principalSid, false
		middleParent.insertAce 0, BasePermission.DELETE, principalSid, true
		child.insertAce 0, BasePermission.DELETE, principalSid, false

		// Explicitly save the changed ACL
		aclService.updateAcl topParent
		aclService.updateAcl middleParent
		aclService.updateAcl child

		// Let's check if we can read them back correctly
		Map<ObjectIdentity, Acl> map = aclService.readAclsById(
				[topParentOid, middleParentOid, childOid])
		assertEquals 3, map.size()

		// Replace our current objects with their retrieved versions
		topParent = map[topParentOid]
		middleParent = map[middleParentOid]
		child = map[childOid]

		// Check the retrieved versions has IDs
		assertNotNull topParent.id
		assertNotNull middleParent.id
		assertNotNull child.id

		// Check their parents were correctly persisted
		assertNull topParent.parentAcl
		assertEquals topParentOid, middleParent.parentAcl.objectIdentity
		assertEquals middleParentOid, child.parentAcl.objectIdentity

		// Check their ACEs were correctly persisted
		assertEquals 2, topParent.entries.size()
		assertEquals 1, middleParent.entries.size()
		assertEquals 1, child.entries.size()

		// Check the retrieved rights are correct
		List<Sid> pSid = [principalSid]

		assertTrue topParent.isGranted(read, pSid, false)
		assertFalse topParent.isGranted(write, pSid, false)
		assertTrue middleParent.isGranted(delete, pSid, false)
		assertFalse child.isGranted(delete, pSid, false)

		shouldFail(NotFoundException) {
			child.isGranted(administration, pSid, false)
		}

		// Now check the inherited rights (when not explicitly overridden) also look OK
		assertTrue child.isGranted(read, pSid, false)
		assertFalse child.isGranted(write, pSid, false)
		assertFalse child.isGranted(delete, pSid, false)

		// Next change the child so it doesn't inherit permissions from above
		child.entriesInheriting = false
		aclService.updateAcl child
		child = aclService.readAclById(childOid)
		assertFalse child.isEntriesInheriting()

		// Check the child permissions no longer inherit
		assertFalse child.isGranted(delete, pSid, true)

		shouldFail(NotFoundException) {
			child.isGranted(read, pSid, true)
		}

		shouldFail(NotFoundException) {
			child.isGranted(write, pSid, true)
		}

		// Let's add an identical permission to the child, but it'll appear AFTER the current permission, so has no impact
		child.insertAce 1, BasePermission.DELETE, principalSid, true

		// Let's also add another permission to the child
		child.insertAce 2, BasePermission.CREATE, principalSid, true

		// Save the changed child
		aclService.updateAcl child
		child = aclService.readAclById(childOid)
		assertEquals 3, child.entries.size()

		// Check the permissions are as they should be
		assertFalse child.isGranted(delete, pSid, true) // as earlier permission overrode
		assertTrue child.isGranted(create, pSid, true)

		// Now check the first ACE (index 0) really is DELETE for our Sid and is non-granting
		AccessControlEntry entry = child.entries[0]
		assertEquals BasePermission.DELETE.mask, entry.permission.mask
		assertEquals principalSid, entry.sid
		assertFalse entry.isGranting()
		assertNotNull entry.id

		// Now delete that first ACE
		child.deleteAce 0

		// Save and check it worked
		child = aclService.updateAcl(child)
		assertEquals 2, child.entries.size()
		assertTrue child.isGranted(delete, pSid, false)
	}

	/**
	 * Demonstrates eviction failure from cache - SEC-676.
	 */
	void testDeleteAclAlsoDeletesChildren() {

		aclService.createAcl topParentOid
		MutableAcl middleParent = aclService.createAcl(middleParentOid)
		MutableAcl child = aclService.createAcl(childOid)
		child.parent = middleParent
		aclService.updateAcl middleParent
		aclService.updateAcl child
		// Check the childOid really is a child of middleParentOid
		Acl childAcl = aclService.readAclById(childOid)

		assertEquals middleParentOid, childAcl.parentAcl.objectIdentity

		// Delete the mid-parent and test if the child was deleted, as well
		aclService.deleteAcl middleParentOid, true

		shouldFail(NotFoundException) {
			aclService.readAclById(middleParentOid)
		}

		shouldFail(NotFoundException) {
			aclService.readAclById(childOid)
		}

		Acl acl = aclService.readAclById(topParentOid)
		assertNotNull acl
		assertEquals acl.objectIdentity, topParentOid
	}

	void testCreateAclRejectsNullParameter() {
		shouldFail(IllegalArgumentException) {
			aclService.createAcl null
		}
	}

	void testCreateAclForADuplicateDomainObject() {
		ObjectIdentity duplicateOid = new ObjectIdentityImpl(Report, 100L)
		aclService.createAcl duplicateOid
		// Try to add the same object second time
		shouldFail(AlreadyExistsException) {
			aclService.createAcl(duplicateOid)
		}
	}

	void testDeleteAclRejectsNullParameters() {
		shouldFail(IllegalArgumentException) {
			aclService.deleteAcl null, true
		}
	}

	void testDeleteAclRemovesRowsFromDatabase() {
		MutableAcl child = aclService.createAcl(childOid)
		child.insertAce 0, BasePermission.DELETE, principalSid, false
		aclService.updateAcl child

		// Remove the child and check all related database rows were removed accordingly
		aclService.deleteAcl childOid, false
		assertEquals 1, AclClass.countByClassName(Report.name)
		assertEquals 0, AclObjectIdentity.count()
		assertEquals 0, AclEntry.count()

		// Check the cache
		assertNull aclCache.getFromCache(childOid)
		assertNull aclCache.getFromCache(102L)
	}

	/**
	 * SEC-655
	 */
	void testChildrenAreClearedFromCacheWhenParentIsUpdated() {

		ObjectIdentity parentOid = new ObjectIdentityImpl(Report, 104L)
		ObjectIdentity childOid = new ObjectIdentityImpl(Report, 105L)

		MutableAcl parent = aclService.createAcl(parentOid)
		MutableAcl child = aclService.createAcl(childOid)

		child.parent = parent
		aclService.updateAcl child

		parent = aclService.readAclById(parentOid)
		parent.insertAce 0, BasePermission.READ, new PrincipalSid('ben'), true
		aclService.updateAcl parent

		parent = aclService.readAclById(parentOid)
		parent.insertAce 1, BasePermission.READ, new PrincipalSid('scott'), true
		aclService.updateAcl parent

		child = aclService.readAclById(childOid)
		parent = child.parentAcl

		assertEquals 'Fails because child has a stale reference to its parent',
					2, parent.entries.size()
		assertEquals 1, parent.entries[0].permission.mask
		assertEquals new PrincipalSid('ben'), parent.entries[0].sid
		assertEquals 1, parent.entries[1].permission.mask
		assertEquals new PrincipalSid('scott'), parent.entries[1].sid
	}

	/**
	 * SEC-655
	 */
	void testChildrenAreClearedFromCacheWhenParentisUpdated2() {
		ObjectIdentityImpl rootObject = new ObjectIdentityImpl(Report, 1L)

		MutableAcl parent = aclService.createAcl(rootObject)
		MutableAcl child = aclService.createAcl(new ObjectIdentityImpl(Report, 2L))
		child.parent = parent
		aclService.updateAcl child

		parent.insertAce 0, BasePermission.ADMINISTRATION,
			new GrantedAuthoritySid('ROLE_ADMINISTRATOR'), true
		aclService.updateAcl parent

		parent.insertAce 1, BasePermission.DELETE, new PrincipalSid('terry'), true
		aclService.updateAcl parent

		child = aclService.readAclById(new ObjectIdentityImpl(Report, 2L))
		parent = child.parentAcl

		assertEquals 2, parent.entries.size()
		assertEquals 16, parent.entries[0].permission.mask
		assertEquals new GrantedAuthoritySid('ROLE_ADMINISTRATOR'), parent.entries[0].sid
		assertEquals 8, parent.entries[1].permission.mask
		assertEquals new PrincipalSid('terry'), parent.entries[1].sid
	}

	void testCumulativePermissions() {
		ObjectIdentity topParentOid = new ObjectIdentityImpl(Report, 110L)
		MutableAcl topParent = aclService.createAcl(topParentOid)

		// Add an ACE permission entry
		Permission cm = new CumulativePermission()
			.set(BasePermission.READ)
			.set(BasePermission.ADMINISTRATION)
		assertEquals 17, cm.mask
		Sid benSid = principalSid
		topParent.insertAce 0, cm, benSid, true
		assertEquals 1, topParent.entries.size()

		// Explicitly save the changed ACL
		topParent = aclService.updateAcl(topParent)

		// Check the mask was retrieved correctly
		assertEquals 17, topParent.entries[0].permission.mask
		assertTrue topParent.isGranted([cm], [benSid], true)
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
		SCH.clearContext()
		ehcacheAclCache.removeAll()
	}
}
