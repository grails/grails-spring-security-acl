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

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
abstract class AbstractAclMappedServiceSpec extends AbstractAclSpec {

	protected service

	void 'getReport() fails when not authenticated'() {
		when:
		def report = Report.get(report1Id)

		then:
		report

		when:
		println "\nSERVICE ${service.getClass().name} ${service.getClass().superclass.name}"
		println "\nREPORT " + service.getReport(report1Id)

		then:
		thrown AuthenticationCredentialsNotFoundException
	}
//
//	void 'getReport() fails when authenticated if the user has no grants for the instance'() {
//
//		given:
//		loginAsUser()
//
//		when:
//		service.getReport report1Id
//
//		then:
//		def e = thrown(AccessDeniedException)
//		e.message.startsWith 'Authentication username has NO permissions to the domain object '
//	}
//
//	void 'getReport() succeeds when authenticated if the user has grants for the instance'() {
//
//		given:
//		loginAsAdmin()
//		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.READ)
//		loginAsUser()
//
//		when:
//		def report = service.getReport(report1Id)
//
//		then:
//		report
//		report1Id == report.id
//	}
//
//	void 'getAllReports() succeeds when authenticated if the user has appropriate grants'() {
//
//		given:
//		loginAsAdmin()
//		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.READ)
//		loginAsUser()
//
//		when:
//		def reports = service.getAllReports()
//
//		then:
//		1 == reports.size()
//		report1Id == reports[0].id
//	}
//
//	void 'updateReport() succeeds when authenticated if the user has appropriate grants'() {
//
//		when:
//		String newName = 'new_name'
//		Report report = Report.get(report1Id)
//
//		then:
//		!report.name.equals(newName)
//
//		when:
//		service.updateReport(report, [name: newName])
//
//		then:
//		// not logged in
//		thrown AuthenticationCredentialsNotFoundException
//
//		when:
//		loginAsAdmin()
//		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.READ)
//
//		loginAsUser()
//		service.updateReport(report, [name: newName])
//
//		then:
//		// no grant for write
//		thrown AccessDeniedException
//
//		when:
//		loginAsAdmin()
//		aclUtilService.addPermission(Report, report1Id, USER, BasePermission.WRITE)
//
//		loginAsUser()
//
//		service.updateReport(report, [name: newName])
//		report = Report.get(report1Id)
//
//		then:
//		newName == report.name
//	}
}
