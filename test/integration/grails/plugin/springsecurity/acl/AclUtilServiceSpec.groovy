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

import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.CumulativePermission

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AclUtilServiceSpec extends AbstractAclSpec {

	def 'permissions let you do things'() {

		when:
			loginAsAdmin()

		then:
			0 == AclClass.count()
			0 == AclEntry.count()
			0 == AclObjectIdentity.count()
			0 == AclSid.count()

		when:
			aclUtilService.addPermission(Report.get(report1Id), USER, BasePermission.READ)

		then:
			1 == AclClass.count()

		when:
			def aclClass = AclClass.list()[0]

		then:
			Report.name == aclClass.className

			2 == AclSid.count()

		when:
			def adminSid = AclSid.list()[0]
			def userSid = AclSid.list()[1]

		then:
			ADMIN == adminSid.sid
			USER == userSid.sid

			1 == AclObjectIdentity.count()

		when:
			def identity = AclObjectIdentity.list()[0]

		then:
			aclClass == identity.aclClass
			report1Id == identity.objectId

			1 == AclEntry.count()

		when:
			def entry = AclEntry.list()[0]

		then:
			userSid == entry.sid
			identity == entry.aclObjectIdentity
			0 == entry.aceOrder
			entry.granting
			1 == entry.mask
	}

	def 'has permission'() {

		given:
			def report = Report.get(report1Id)
			loginAsAdmin()

			def userAuth = createUserAuth()

		when:
			aclUtilService.addPermission(report, USER, BasePermission.READ)

		then:
			aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
			!aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
			!aclUtilService.hasPermission(userAuth, Report.get(report2Id), BasePermission.READ)

		when:
			aclUtilService.addPermission(report, USER, BasePermission.WRITE)

		then:
			aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
			aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
			!aclUtilService.hasPermission(userAuth, Report.get(report2Id), BasePermission.READ)
	}

	def 'delete permission'() {

		given:
			def report = Report.get(report1Id)
			loginAsAdmin()

			def userAuth = createUserAuth()

		when:
			aclUtilService.addPermission(report, USER, BasePermission.READ)
			aclUtilService.addPermission(report, USER, BasePermission.WRITE)

		then:
			aclUtilService.hasPermission userAuth, report, BasePermission.READ
			aclUtilService.hasPermission userAuth, report, BasePermission.WRITE

		when:
			aclUtilService.deletePermission(report, USER, BasePermission.READ)

		then:
			!aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
			aclUtilService.hasPermission userAuth, report, BasePermission.WRITE
	}

	def 'cumulative permission'() {

		given:
			def report = Report.get(report1Id)
			loginAsAdmin()

			def userAuth = createUserAuth()

		when:

			aclUtilService.addPermission(report, USER, new CumulativePermission()
				.set(BasePermission.READ)
				.set(BasePermission.WRITE))

		then:
			!aclUtilService.hasPermission(userAuth, report, BasePermission.READ)
			!aclUtilService.hasPermission(userAuth, report, BasePermission.WRITE)
			!aclUtilService.hasPermission(userAuth, Report.get(report2Id), BasePermission.READ)
	}
}
