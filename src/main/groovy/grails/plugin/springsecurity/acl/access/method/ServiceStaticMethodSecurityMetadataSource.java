/* Copyright 2009-2014 SpringSource.
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
package grails.plugin.springsecurity.acl.access.method;

import grails.plugin.springsecurity.acl.util.ProxyUtils;

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

	protected final Map<String, Map<String, List<ConfigAttribute>>> methodConfigs =
		new HashMap<String, Map<String,List<ConfigAttribute>>>();
	protected final Map<String, List<ConfigAttribute>> classConfigs =
		new HashMap<String, List<ConfigAttribute>>();

	@Override
	protected Collection<ConfigAttribute> findAttributes(final Class<?> clazz) {
		return classConfigs.get(ProxyUtils.unproxy(clazz).getName());
	}

	@Override
	protected Collection<ConfigAttribute> findAttributes(final Method method, final Class<?> targetClass) {
		Class<?> actualClass = ProxyUtils.unproxy(targetClass);
		Method actualMethod = ProxyUtils.unproxy(method);
		Map<String, List<ConfigAttribute>> configs = methodConfigs.get(actualClass.getName());
		return configs == null ? null : configs.get(actualMethod.getName());
	}

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

		populateMap(classConfigs, classConfigNames);
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
			methodConfigs.put(entry.getKey(), configs);
		}
	}

	protected void populateMap(final Map<String, List<ConfigAttribute>> dest,
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
