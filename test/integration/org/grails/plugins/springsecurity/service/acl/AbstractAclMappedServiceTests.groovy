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

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
abstract class AbstractAclMappedServiceTests extends AbstractAclTest {

	protected service

	void testGetReportNoAuth() {
		assertNotNull Report.get(report1Id)
		shouldFail(AuthenticationCredentialsNotFoundException) {
			service.getReport(report1Id)
		}
	}

	void testGetReportAuthNoGrants() {
		loginAsUser()

		String message = shouldFail(AccessDeniedException) {
			service.getReport report1Id
		}
		assertTrue message.startsWith('Authentication username has NO permissions to the domain object ')
	}

	void testGetReportAuthGrants() {
		loginAsAdmin()

		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.READ)

		loginAsUser()

		def report = service.getReport(report1Id)
		assertNotNull report
		assertEquals report1Id, report.id
	}

	void testGetAllReports() {
		loginAsAdmin()

		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.READ)

		loginAsUser()

		def reports = service.getAllReports()
		assertEquals 1, reports.size()
		assertEquals report1Id, reports[0].id
	}

	void testUpdateReport() {

		String newName = 'new_name'

		Report report = Report.get(report1Id)
		assertFalse report.name.equals(newName)

		// not logged in
		shouldFail(AuthenticationCredentialsNotFoundException) {
			service.updateReport(report, [name: newName])
		}

		loginAsAdmin()
		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.READ)

		loginAsUser()

		// no grant for write
		shouldFail(AccessDeniedException) {
			service.updateReport(report, [name: newName])
		}

		loginAsAdmin()
		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.WRITE)

		loginAsUser()

		service.updateReport(report, [name: newName])
		report = Report.get(report1Id)
		assertEquals newName, report.name
	}
}
