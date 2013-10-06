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
package grails.plugin.springsecurity.acl

import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.CumulativePermission

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclUtilServiceTests extends AbstractAclTest {

	void testAddPermission() {
		loginAsAdmin()

		assertEquals 0, AclClass.count()
		assertEquals 0, AclEntry.count()
		assertEquals 0, AclObjectIdentity.count()
		assertEquals 0, AclSid.count()

		aclUtilService.addPermission(Report.get(report1Id), USER, BasePermission.READ)

		assertEquals 1, AclClass.count()
		def aclClass = AclClass.list()[0]
		assertEquals Report.name, aclClass.className

		assertEquals 2, AclSid.count()
		def adminSid = AclSid.list()[0]
		def userSid = AclSid.list()[1]
		assertEquals ADMIN, adminSid.sid
		assertEquals USER, userSid.sid

		assertEquals 1, AclObjectIdentity.count()
		def identity = AclObjectIdentity.list()[0]
		assertEquals aclClass, identity.aclClass
		assertEquals report1Id, identity.objectId

		assertEquals 1, AclEntry.count()
		def entry = AclEntry.list()[0]
		assertEquals userSid, entry.sid
		assertEquals identity, entry.aclObjectIdentity
		assertEquals 0, entry.aceOrder
		assertTrue entry.granting
		assertEquals 1, entry.mask
	}

	void testHasPermission() {
		def report = Report.get(report1Id)
		loginAsAdmin()

		def userAuth = createUserAuth()

		aclUtilService.addPermission(report, USER, BasePermission.READ)
		assertTrue aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
		assertFalse aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
		assertFalse aclUtilService.hasPermission(userAuth, Report.get(report2Id), BasePermission.READ)

		aclUtilService.addPermission(report, USER, BasePermission.WRITE)
		assertTrue aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
		assertTrue aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
		assertFalse aclUtilService.hasPermission(userAuth, Report.get(report2Id), BasePermission.READ)
	}

	void testDeletePermission() {
		def report = Report.get(report1Id)
		loginAsAdmin()

		def userAuth = createUserAuth()

		aclUtilService.addPermission(report, USER, BasePermission.READ)
		aclUtilService.addPermission(report, USER, BasePermission.WRITE)
		assertTrue aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
		assertTrue aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)

		aclUtilService.deletePermission(report, USER, BasePermission.READ)
		assertFalse aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
		assertTrue aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
	}

	void testCumulativePermission() {
		def report = Report.get(report1Id)
		loginAsAdmin()

		def userAuth = createUserAuth()

		aclUtilService.addPermission(report, USER, new CumulativePermission()
			.set(BasePermission.READ)
			.set(BasePermission.WRITE))

		assertFalse aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
		assertFalse aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
		assertFalse aclUtilService.hasPermission(userAuth, Report.get(report2Id), BasePermission.READ)
	}
}
