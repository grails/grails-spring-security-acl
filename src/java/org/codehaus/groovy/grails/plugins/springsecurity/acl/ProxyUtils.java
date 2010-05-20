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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility methods for unproxying transactional services.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ProxyUtils {

	private ProxyUtils() {
		// static only
	}

	/**
	 * Finds the unproxied superclass if proxied.
	 * @param clazz  the potentially proxied class
	 * @return  the unproxied class
	 */
	public static Class<?> unproxy(final Class<?> clazz) {
		Class<?> current = clazz;
		while (AopUtils.isCglibProxyClass(current)) {
			current = current.getSuperclass();
		}
		return current;
	}

	/**
	 * Finds the method in the unproxied superclass if proxied.
	 * @param method  the method
	 * @return  the method in the unproxied class
	 */
	public static Method unproxy(final Method method) {
		Class<?> clazz = method.getDeclaringClass();

		if (!AopUtils.isCglibProxyClass(clazz)) {
			return method;
		}

		return ReflectionUtils.findMethod(unproxy(clazz), method.getName(),
				method.getParameterTypes());
	}

	/**
	 * Finds the constructor in the unproxied superclass if proxied.
	 * @param constructor  the constructor
	 * @return  the constructor in the unproxied class
	 */
	public static Constructor<?> unproxy(final Constructor<?> constructor) {
		Class<?> clazz = constructor.getDeclaringClass();

		if (!AopUtils.isCglibProxyClass(clazz)) {
			return constructor;
		}

		Class<?> searchType = unproxy(clazz);
		while (searchType != null) {
			for (Constructor<?> c : searchType.getConstructors()) {
				if (constructor.getName().equals(c.getName())
						&& (constructor.getParameterTypes() == null ||
								Arrays.equals(constructor.getParameterTypes(), c.getParameterTypes()))) {
					return c;
				}
			}
			searchType = searchType.getSuperclass();
		}

		return null;
	}
}
