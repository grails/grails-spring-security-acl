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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.method.AbstractFallbackMethodSecurityMetadataSource;

/**
 * <code>MethodSecurityMetadataSource</code> that's populated by 'springSecurityACL' blocks in services.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ServiceStaticMethodSecurityMetadataSource extends AbstractFallbackMethodSecurityMetadataSource {

	private final Map<String, Map<String, List<ConfigAttribute>>> _methodConfigs =
		new HashMap<String, Map<String,List<ConfigAttribute>>>();
	private final Map<String, List<ConfigAttribute>> _classConfigs =
		new HashMap<String, List<ConfigAttribute>>();

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.method.AbstractFallbackMethodSecurityMetadataSource#findAttributes(
	 * 	java.lang.Class)
	 */
	@Override
	protected Collection<ConfigAttribute> findAttributes(final Class<?> clazz) {
		return _classConfigs.get(ProxyUtils.unproxy(clazz).getName());
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.method.AbstractFallbackMethodSecurityMetadataSource#findAttributes(
	 * 	java.lang.reflect.Method, java.lang.Class)
	 */
	@Override
	protected Collection<ConfigAttribute> findAttributes(final Method method, final Class<?> targetClass) {
		Class<?> actualClass = ProxyUtils.unproxy(targetClass);
		Method actualMethod = ProxyUtils.unproxy(method);
		Map<String, List<ConfigAttribute>> configs = _methodConfigs.get(actualClass.getName());
		return configs == null ? null : configs.get(actualMethod.getName());
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.SecurityMetadataSource#getAllConfigAttributes()
	 */
	public Collection<ConfigAttribute> getAllConfigAttributes() {
		return null;
	}

	/**
	 * Dependency injection for class-scope rules.
	 * @param classConfigNames  keys are class names, values are one or more tokens to apply,
	 * e.g. ROLE_ADMIN, AFTER_ACL_COLLECTION_READ.
	 */
	public void setClassConfigNames(final Map<String, List<String>> classConfigNames) {
		if (classConfigNames == null) {
			return;
		}

		populateMap(_classConfigs, classConfigNames);
	}

	/**
	 * Dependency injection for method-scope rules.
	 * @param methodConfigNames  keys are class names, values are maps with method name keys and
	 * values are one or more tokens to apply, e.g. ROLE_ADMIN, AFTER_ACL_COLLECTION_READ.
	 */
	public void setMethodConfigNames(final Map<String, Map<String, List<String>>> methodConfigNames) {
		for (Map.Entry<String, Map<String, List<String>>> entry : methodConfigNames.entrySet()) {
			Map<String, List<ConfigAttribute>> configs = new HashMap<String, List<ConfigAttribute>>();
			populateMap(configs, entry.getValue());
			_methodConfigs.put(entry.getKey(), configs);
		}
	}

	private void populateMap(final Map<String, List<ConfigAttribute>> dest,
			final Map<String, List<String>> source) {

		for (Map.Entry<String, List<String>> entry : source.entrySet()) {
			List<ConfigAttribute> configs = new ArrayList<ConfigAttribute>();
			for (String config : entry.getValue()) {
				configs.add(new SecurityConfig(config));
			}
			dest.put(entry.getKey(), configs);
		}
	}
}
