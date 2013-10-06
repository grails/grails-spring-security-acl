/* Copyright 2009-2013 SpringSource.
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
package test

import org.springframework.security.access.annotation.Secured

import test.TestReport as Report

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class TestSpringAnnotatedService {

	@Secured(['ROLE_USER', 'AFTER_ACL_READ'])
	Report getReport(long id) {
		Report.get id
	}

	Report createReport(params) {
		Report report = new Report(params)
		report.save()
		report
	}

	@Secured(['ROLE_USER', 'AFTER_ACL_COLLECTION_READ'])
	List<Report> getAllReports() { Report.list() }

	@Secured(['ROLE_USER', 'ROLE_ADMIN'])
	String getReportName(long id) { Report.get(id).name }

	@Secured(['ACL_REPORT_WRITE'])
	Report updateReport(Report report, params) {
		report.properties = params
		report.save()
		report
	}
}
