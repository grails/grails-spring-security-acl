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

import grails.plugins.springsecurity.acl.AbstractAclObjectIdentity

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclObjectIdentity extends AbstractAclObjectIdentity {
	
	static {
		println "PLUGIN -----------------> AclObjectIdentity"
	}
	
	/* This change should be unnecessary because Spring Security ACL allows overriding
	 * the Domain classes, but for some reason, my version of the domain classes are not
	 * being called so I've had to override in the already forked plugin.
	 *
	 * I have tried many different approaches to getting my Domain class to be used:
	 * • grails clean
	 * • rm -rf ~/.grails
	 * • removing forked plugin and using BuildConfig dependency
	 */
	String objectId

	@Override
	String toString() {
		"AclObjectIdentity id $id, aclClass $aclClass.className, " +
		"objectId $objectId, entriesInheriting $entriesInheriting"
	}

	static mapping = {
		version false
		aclClass column: 'object_id_class'
		owner column: 'owner_sid'
		parent column: 'parent_object'
		objectId column: 'object_id_identity'
	}

	static constraints = {
		objectId unique: 'aclClass'
	}
}
