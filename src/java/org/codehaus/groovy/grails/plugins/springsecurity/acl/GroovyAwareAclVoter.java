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
package org.codehaus.groovy.grails.plugins.springsecurity.acl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

/**
 * Dummy voter that votes yes on Groovy methods like getMetaClass() since they wouldn't be
 * secured but if all voters abstain access is denied.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class GroovyAwareAclVoter implements AccessDecisionVoter {

	private static final List<String> NON_SECURABLE_METHODS = new ArrayList<String>(Arrays.asList(
			"invokeMethod", "getMetaClass", "setMetaClass", "getProperty", "setProperty",
			"isTransactional", "getTransactional", "setTransactional"));
	static {
		for (Method m : Object.class.getMethods()) {
			NON_SECURABLE_METHODS.add(m.getName());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.AccessDecisionVoter#supports(org.springframework.security.access.ConfigAttribute)
	 */
	public boolean supports(final ConfigAttribute attribute) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.AccessDecisionVoter#supports(java.lang.Class)
	 */
	public boolean supports(final Class<?> clazz) {
		return clazz.isAssignableFrom(MethodInvocation.class);
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.AccessDecisionVoter#vote(org.springframework.security.core.Authentication, java.lang.Object, java.util.Collection)
	 */
	public int vote(final Authentication authentication, final Object object,
			final Collection<ConfigAttribute> attributes) {

		if (NON_SECURABLE_METHODS.contains(((MethodInvocation)object).getMethod().getName())) {
			return ACCESS_GRANTED;
		}

		return ACCESS_ABSTAIN;
	}
}
