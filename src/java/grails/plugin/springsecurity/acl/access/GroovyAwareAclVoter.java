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
package grails.plugin.springsecurity.acl.access;

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
public class GroovyAwareAclVoter implements AccessDecisionVoter<MethodInvocation> {

	protected static final List<String> NON_SECURABLE_METHODS = new ArrayList<String>(Arrays.asList(
			"invokeMethod", "getMetaClass", "setMetaClass", "getProperty", "setProperty",
			"isTransactional", "getTransactional", "setTransactional"));
	static {
		for (Method m : Object.class.getMethods()) {
			NON_SECURABLE_METHODS.add(m.getName());
		}
	}

	public boolean supports(final ConfigAttribute attribute) {
		return true;
	}

	public boolean supports(final Class<?> clazz) {
		return clazz.isAssignableFrom(MethodInvocation.class);
	}

	public int vote(final Authentication authentication, final MethodInvocation object, final Collection<ConfigAttribute> attributes) {
		if (NON_SECURABLE_METHODS.contains(object.getMethod().getName())) {
			return ACCESS_GRANTED;
		}

		return ACCESS_ABSTAIN;
	}
}
