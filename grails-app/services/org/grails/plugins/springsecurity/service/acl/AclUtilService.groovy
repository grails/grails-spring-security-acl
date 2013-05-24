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

import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.springframework.security.acls.domain.AccessControlEntryImpl
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Permission
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.Authentication

/**
 * Utility service that hides a lot of the implementation details for working with ACLs.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclUtilService {

	/** Dependency injection for aclService. */
	def aclService

	/** Dependency injection for permissionEvaluator. */
	def permissionEvaluator

	/** Dependency injection for sidRetrievalStrategy. */
	def sidRetrievalStrategy

	/** Dependency injection for objectIdentityRetrievalStrategy. */
	def objectIdentityRetrievalStrategy

	/**
	 * Grant a permission. Used when you don't have the instance available.
	 *
	 * @param domainClass  the domain class
	 * @param id  the instance id
	 * @param recipient  the grantee; can be a username, role name, Sid, or Authentication
	 * @param permission  the permission to grant
	 */
	void addPermission(Class<?> domainClass, long id, recipient, Permission permission) {
		ObjectIdentity oid = objectIdentityRetrievalStrategy.createObjectIdentity(id, domainClass.name)
		addPermission oid, recipient, permission
	}

	/**
	 * Grant a permission. Used when you have the instance available.
	 *
	 * @param domainObject  the domain class instance
	 * @param recipient  the grantee; can be a username, role name, Sid, or Authentication
	 * @param permission  the permission to grant
	 */
	void addPermission(domainObject, recipient, Permission permission) {
		ObjectIdentity oid = objectIdentityRetrievalStrategy.getObjectIdentity(domainObject)
		addPermission oid, recipient, permission
	}

    /**
     * Test to see if an arbitrary domainObject has a permission for the recipient
     * This is different from hasPermission() in that hasPermission() will only
     * test permissions against the currently authenticated entity.
     *
     * @param domainObject
     * @param recipient
     * @param permission
     * @return false - returns false if the ACL on the domain does not contain the permission
     */
    boolean checkPermission(domainObject, recipient, Permission permission){
        ObjectIdentity oid = objectIdentityRetrievalStrategy.getObjectIdentity(domainObject)

        Sid sid = createSid(recipient)
        def acl = null

        try {
            acl = aclService.readAclById(oid, [sid])
        }
        catch (NotFoundException e) {
            log.info("checkPermission did not find ACL for domain : " + domainObject)
        }

        if (acl == null)
            return false

        def entryList = acl.entries
        for (int i=0; i < entryList.size(); i++){

            if ((entryList[i].sid == sid) && (permission.mask == entryList[i].permission.mask))
                return true;

            log.info(entryList[i])
        }

        return false
    }


    /**
     * This method counts the total number of domain objects which has permissions
     * matching @permission
     *
     * @domainObjectClass - the class of domain object to test
     * @roles - List of Authority instances
     * @permission - the permission to test for
     * @return Integer - total count of objects with the specified permission.
     */
    Integer countWithPermission(domainObjectClass, roles, Permission permission){

        //FIXME: This method assumes all permissions are GrantedAuthority permissions (role-based).
        //       Add support for Principal permissions

        def sidList = []

        roles.each { role->

            GrantedAuthoritySid sid = createSid(role.authority)
            sidList.add(sid.grantedAuthority)
        }

        //count the number of aclEntries for a specific class which has matching permissions
        //for the sid

        def aclEntries = AclEntry.executeQuery (
                "SELECT COUNT(DISTINCT id) FROM AclEntry " +
                "WHERE aclObjectIdentity.aclClass.className = :className " +
                 " AND mask = :permission " +
                 " AND granting = true " +
                 " AND sid.sid IN :sidList",
                [className:  domainObjectClass, permission: permission.mask, sidList: sidList]
        )

        def retVal = aclEntries.size() > 0 ? aclEntries[0] : 0
        return retVal
    }


    /**
     * This method counts the total number of domain objects which has any of the permissions
     * listed in @permissions
     *
     * @domainObjectClass - the class of domain object to test
     * @roles - List of Authority objects
     * @permissions - List of permissions to test
     * @return Integer - total count of objects with the specified permission.
     */
    Integer countWithPermissionList(domainObjectClass, roles, List<Permission> permissions){

        //FIXME: This method assumes all permissions are GrantedAuthority permissions (role-based).
        //       Add support for Principal permissions

        def sidList = [], permissionList = []

        permissions.each { permission ->
            permissionList.add(permission.mask)
        }

        roles.each { role->

            GrantedAuthoritySid sid = createSid(role.authority)
            sidList.add(sid.grantedAuthority)
        }

        //count the number of aclEntries for a specific class which has matching permissions
        //for the sid

        def aclEntries = AclEntry.executeQuery (
                "SELECT COUNT(DISTINCT id) FROM AclEntry " +
                "WHERE aclObjectIdentity.aclClass.className = :className " +
                 " AND mask IN :permissions " +
                 " AND granting = true " +
                 " AND sid.sid IN :sidList",
                [className:  domainObjectClass, permissions: permissionList, sidList: sidList]
        )

        def retVal = aclEntries.size() > 0 ? aclEntries[0] : 0
        return retVal
    }

    /**
     * find all domain objects which have the given permission for the stated role(authority)
     * It's important to note that this method will return a list of "ids". i.e you still
     * need to perform a DomainObject.get(objectIdentities[x]) to get back your domain object
     * of class @domainObjectClass
     *
     * @domainObjectClass - the class name of the domain object
     * @role - the authority/role name of the grantedAuthority
     * @permission - the specific permission we are checking for
     */
    def findWithPermission(domainObjectClass, role, Permission permission){

        GrantedAuthoritySid sid = createSid(role)

        def objectIdentities = AclEntry.executeQuery (
                "SELECT aclObjectIdentity.objectId FROM AclEntry " +
                "WHERE aclObjectIdentity.aclClass.className = :className " +
                 " AND mask = :permission " +
                 " AND granting = true " +
                 " AND sid.sid = :sid",
                [className:  domainObjectClass, permission: permission.mask, sid: sid.grantedAuthority]
        )

        return objectIdentities
    }


	/**
	 * Grant a permission.
	 *
	 * @param oid  represents the domain object
	 * @param recipient  the grantee; can be a username, role name, Sid, or Authentication
	 * @param permission  the permission to grant
	 */
	void addPermission(ObjectIdentity oid, recipient, Permission permission) {

		Sid sid = createSid(recipient)

		def acl
		try {
			acl = aclService.readAclById(oid)
		}
		catch (NotFoundException e) {
			acl = aclService.createAcl(oid)
		}

		acl.insertAce acl.entries.size(), permission, sid, true
		aclService.updateAcl acl

		log.debug "Added permission $permission for Sid $sid for $oid.type with id $oid.identifier"
	}

	/**
	 * Update the owner of the domain class instance.
	 *
	 * @param domainObject  the domain class instance
	 * @param newOwnerUsername  the new username
	 */
	void changeOwner(domainObject, String newUsername) {
		def acl = readAcl(domainObject)
		acl.owner = new PrincipalSid(newUsername)
		aclService.updateAcl acl
	}

	/**
	 * Removes a granted permission. Used when you have the instance available.
	 *
	 * @param domainObject  the domain class instance
	 * @param recipient  the grantee; can be a username, role name, Sid, or Authentication
	 * @param permission  the permission to remove
	 */
	void deletePermission(domainObject, recipient, Permission permission) {
		deletePermission domainObject.getClass(), domainObject.id, recipient, permission
	}

	/**
	 * Removes a granted permission. Used when you don't have the instance available.
	 *
	 * @param domainClass  the domain class
	 * @param id  the instance id
	 * @param recipient  the grantee; can be a username, role name, Sid, or Authentication
	 * @param permission  the permission to remove
	 */
	void deletePermission(Class<?> domainClass, long id, recipient, Permission permission) {
		Sid sid = createSid(recipient)
		def acl = readAcl(domainClass, id)

		acl.entries.eachWithIndex { entry, i ->
			if (entry.sid.equals(sid) && entry.permission.equals(permission)) {
				acl.deleteAce i
			}
		}

		aclService.updateAcl acl

		log.debug "Deleted ${domainClass.name}($id) ACL permissions for recipient $recipient"
	}

	/**
	 * Check if the authentication has grants for the specified permission(s) on the domain class instance.
	 *
	 * @param authentication  an authentication representing a user and roles
	 * @param domainObject  the domain class instance
	 * @param permissions  one or more permissions to check
	 * @return  <code>true</code> if granted
	 */
	boolean hasPermission(Authentication authentication, domainObject, Permission... permissions) {
		permissionEvaluator.hasPermission authentication, domainObject, permissions
	}

	/**
	 * Check if the authentication has grants for the specified permission(s) on the domain class instance.
	 *
	 * @param authentication  an authentication representing a user and roles
	 * @param domainObject  the domain class instance
	 * @param permissions  one or more permissions to check
	 * @return  <code>true</code> if granted
	 */
	boolean hasPermission(Authentication authentication, domainObject, List<Permission> permissions) {
		hasPermission authentication, domainObject, permissions as Permission[]
	}

	/**
	 * Helper method to retrieve the ACL for a domain class instance.
	 *
	 * @param domainObject  the domain class instance
	 * @return the {@link Acl} (never <code>null</code>)
	 */
	Acl readAcl(domainObject) {
		aclService.readAclById objectIdentityRetrievalStrategy.getObjectIdentity(domainObject)
	}

	/**
	 * Helper method to retrieve the ACL for a domain class instance.
	 *
	 * @param domainClass  the domain class
	 * @param id  the instance id
	 * @return the {@link Acl} (never <code>null</code>)
	 */
	Acl readAcl(Class<?> domainClass, id) {
		aclService.readAclById objectIdentityRetrievalStrategy.createObjectIdentity(id, domainClass.name)
	}

	/**
	 * Helper method to delete an ACL for a domain class.
	 *
	 * @param domainObject  the domain class instance
	 */
	void deleteAcl(domainObject) {
		aclService.deleteAcl objectIdentityRetrievalStrategy.getObjectIdentity(domainObject), false
	}

	protected Sid createSid(recipient) {
		if (recipient instanceof String) {
			return recipient.startsWith('ROLE_') ?
					new GrantedAuthoritySid(recipient) :
					new PrincipalSid(recipient)
		}

		if (recipient instanceof Sid) {
			return recipient
		}

		if (recipient instanceof Authentication) {
			return new PrincipalSid(recipient)
		}

		throw new IllegalArgumentException('recipient must be a String, Sid, or Authentication')
	}
}
