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
package org.codehaus.groovy.grails.plugins.springsecurity.acl

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.security.core.userdetails.User

/**
 * Integration tests for run-as functionality.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class RunAsTests extends GroovyTestCase {

	def filterInvocationInterceptor
	def testRunAsService
	def testSecureService

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
		SCH.clearContext()
	}

	void testNotAuthenticated() {
		authenticate 'ROLE_ANONYMOUS'

		shouldFail(AccessDeniedException) {
			testRunAsService.method1()
		}

		shouldFail(AccessDeniedException) {
			testRunAsService.method2()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method1()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method2()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method3()
		}
	}

	void testAuthenticatedAdmin() {
		authenticate 'ROLE_ADMIN'

		shouldFail(AccessDeniedException) {
			testSecureService.method1()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method2()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method3()
		}

		assertEquals 'method1', testRunAsService.method1()
		assertEquals 'method2', testRunAsService.method2()
	}

	void testAuthenticatedUser() {
		authenticate 'ROLE_USER'

		shouldFail(AccessDeniedException) {
			testRunAsService.method1()
		}

		shouldFail(AccessDeniedException) {
			testRunAsService.method2()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method1()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method2()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method3()
		}
	}

	void testAuthenticatedSuperuser() {
		authenticate 'ROLE_SUPERUSER'

		shouldFail(AccessDeniedException) {
			testRunAsService.method1()
		}

		shouldFail(AccessDeniedException) {
			testRunAsService.method2()
		}

		shouldFail(AccessDeniedException) {
			testSecureService.method1()
		}

		assertEquals 'method2', testSecureService.method2()
		assertEquals 'method3', testSecureService.method3()
	}

	private void authenticate(roles) {
		def authorities = SpringSecurityUtils.parseAuthoritiesString(roles)
		def principal = new User('username', 'password', true, true, true, true, authorities)
		def authentication = new TestingAuthenticationToken(principal, null, authorities)
		authentication.authenticated = true
		SCH.context.authentication = authentication
	}
}
