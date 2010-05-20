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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.method.AbstractMethodSecurityMetadataSource;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Replacement for DelegatingMethodSecurityMetadataSource which is final.
 * <p/>
 * Makes two changes; unproxies classes, and treats an empty return value from
 * <code>MethodSecurityMetadataSource.getAttributes()</code> as equivalent to a
 * <code>null</code> return since an empty return stops the loop and another
 * source might have attributes.
 *
 * @author Ben Alex
 * @author Luke Taylor
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ProxyAwareDelegatingMethodSecurityMetadataSource
       extends AbstractMethodSecurityMetadataSource
       implements InitializingBean {

	private final static List<ConfigAttribute> NULL_CONFIG_ATTRIBUTE = Collections.emptyList();

	private List<MethodSecurityMetadataSource> _methodSecurityMetadataSources;
	private final Map<DefaultCacheKey, Collection<ConfigAttribute>> _cache =
		new HashMap<DefaultCacheKey, Collection<ConfigAttribute>>();

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.method.MethodSecurityMetadataSource#getAttributes(
	 * 	java.lang.reflect.Method, java.lang.Class)
	 */
	public Collection<ConfigAttribute> getAttributes(final Method m, final Class<?> tc) {

		Method method = ProxyUtils.unproxy(m);
		Class<?> targetClass = ProxyUtils.unproxy(tc);

		DefaultCacheKey cacheKey = new DefaultCacheKey(method, targetClass);
		synchronized (_cache) {
			Collection<ConfigAttribute> cached = _cache.get(cacheKey);
			// Check for canonical value indicating there is no config attribute,
			if (cached == NULL_CONFIG_ATTRIBUTE) {
				return null;
			}

			if (cached != null) {
				return cached;
			}

			// No cached value, so query the sources to find a result
			Collection<ConfigAttribute> attributes = null;
			for (MethodSecurityMetadataSource s : _methodSecurityMetadataSources) {
				attributes = s.getAttributes(method, targetClass);
				if (attributes != null && !attributes.isEmpty()) {
					break;
				}
			}

			// Put it in the cache.
			if (attributes == null) {
				_cache.put(cacheKey, NULL_CONFIG_ATTRIBUTE);
				return null;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Adding security method [" + cacheKey + "] with attributes " + attributes);
			}

			_cache.put(cacheKey, attributes);

			return attributes;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.access.SecurityMetadataSource#getAllConfigAttributes()
	 */
	public Collection<ConfigAttribute> getAllConfigAttributes() {
		Set<ConfigAttribute> set = new HashSet<ConfigAttribute>();
		for (MethodSecurityMetadataSource s : _methodSecurityMetadataSources) {
			Collection<ConfigAttribute> attrs = s.getAllConfigAttributes();
			if (attrs != null) {
				set.addAll(attrs);
			}
		}
		return set;
	}

	/**
	 * Dependency injection for the sources.
	 * @param sources  the sources
	 */
	public void setMethodSecurityMetadataSources(final List<MethodSecurityMetadataSource> sources) {
		_methodSecurityMetadataSources = sources;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		Assert.notEmpty(_methodSecurityMetadataSources, "A list of MethodSecurityMetadataSources is required");
	}

	private static class DefaultCacheKey {
		private final Method _method;
		private final Class<?> _targetClass;

		DefaultCacheKey(final Method method, final Class<?> targetClass) {
			_method = method;
			_targetClass = targetClass;
		}

		@Override
		public boolean equals(final Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof DefaultCacheKey)) {
				return false;
			}
			DefaultCacheKey otherKey = (DefaultCacheKey) other;
			return (_method.equals(otherKey._method) &&
					ObjectUtils.nullSafeEquals(_targetClass, otherKey._targetClass));
		}

		@Override
		public int hashCode() {
			return _method.hashCode() * 21 + (_targetClass != null ? _targetClass.hashCode() : 0);
		}

		@Override
		public String toString() {
			return "CacheKey[" + (_targetClass == null ? "-" : _targetClass.getName()) + "; " + _method + "]";
		}
	}
}
