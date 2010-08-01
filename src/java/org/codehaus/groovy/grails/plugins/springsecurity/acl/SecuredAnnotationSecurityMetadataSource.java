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

import grails.plugins.springsecurity.Secured;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.method.AbstractFallbackMethodSecurityMetadataSource;

/**
 * Based on the Spring Security class of the same name but supports the plugin's @Secured annotation
 * which can be applied on fields to support annotated controller actions.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class SecuredAnnotationSecurityMetadataSource extends AbstractFallbackMethodSecurityMetadataSource {

	private Collection<String> _serviceClassNames;

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.method.AbstractFallbackMethodSecurityMetadataSource#findAttributes(
	 * 	java.lang.Class)
	 */
	@Override
	protected Collection<ConfigAttribute> findAttributes(final Class<?> clazz) {

		Class<?> actualClass = ProxyUtils.unproxy(clazz);

		if (!isService(actualClass)) {
			return null;
		}

		return processAnnotation(actualClass.getAnnotation(Secured.class));
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.method.AbstractFallbackMethodSecurityMetadataSource#findAttributes(
	 * 	java.lang.reflect.Method, java.lang.Class)
	 */
	@Override
	protected Collection<ConfigAttribute> findAttributes(final Method method, final Class<?> targetClass) {
		Method actualMethod = ProxyUtils.unproxy(method);

		if (!isService(actualMethod.getDeclaringClass())) {
			return null;
		}

		return processAnnotation(actualMethod.getAnnotation(Secured.class));
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.SecurityMetadataSource#getAllConfigAttributes()
	 */
	public Collection<ConfigAttribute> getAllConfigAttributes() {
		return null;
	}

	protected List<ConfigAttribute> processAnnotation(final Annotation a) {
		if (!(a instanceof Secured)) {
			return null;
		}

		String[] attributeTokens = ((Secured)a).value();
		List<ConfigAttribute> attributes = new ArrayList<ConfigAttribute>(attributeTokens.length);
		for(String token : attributeTokens) {
			attributes.add(new SecurityConfig(token));
		}

		return attributes;
	}

	protected boolean isService(final Class<?> clazz) {
		for (String name : _serviceClassNames) {
			if (name.equals(clazz.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Dependency injection for current service class names.
	 * @param serviceClassNames  the names
	 */
	public void setServiceClassNames(final Collection<String> serviceClassNames) {
		_serviceClassNames = serviceClassNames;
	}
}
