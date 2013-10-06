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
package grails.plugin.springsecurity.acl

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class TestClassAnnotatedServiceTests extends AbstractAclTest {

	def testClassAnnotatedService

	void testNotAnnotated() {

		shouldFail(AuthenticationCredentialsNotFoundException) {
			testClassAnnotatedService.notAnnotated()
		}

		loginAsUser()
		shouldFail(AccessDeniedException) {
			testClassAnnotatedService.notAnnotated()
		}

		loginAsAdmin()
		testClassAnnotatedService.notAnnotated()
	}

	void testOverrideUserAnnotated() {
		shouldFail(AuthenticationCredentialsNotFoundException) {
			testClassAnnotatedService.userAnnotated()
		}

		loginAsAdmin()
		shouldFail(AccessDeniedException) {
			testClassAnnotatedService.userAnnotated()
		}

		loginAsUser()
		testClassAnnotatedService.userAnnotated()
	}
}
