/* Copyright 2011 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;

/**
 * Borrowed from https://github.com/alkemist/grails-aop-reloading-fix/blob/master/src/groovy/grails/plugin/aopreloadingfix/ClassLoaderPerProxyGroovyAwareAspectJAwareAdvisorAutoProxyCreator.groovy
 *
 * @author Luke Daley
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ClassLoaderPerProxyBeanNameAutoProxyCreator extends BeanNameAutoProxyCreator {

	private static final long serialVersionUID = 1;

	private ClassLoader baseLoader;
	private Collection<String> beanNames;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		super.setBeanClassLoader(classLoader);
		baseLoader = classLoader;
	}

	@Override
	protected Object createProxy(Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {
		setProxyClassLoader(new GrailsClassLoader(baseLoader, null, null));
		Object proxy = super.createProxy(beanClass, beanName, specificInterceptors, targetSource);
		setProxyClassLoader(baseLoader);
		return proxy;
	}

	@Override
	protected Object getCacheKey(Class<?> beanClass, String beanName) {
		return beanClass.hashCode() + "_" + beanName;
	}

	@Override
	protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
		return beanNames.contains(beanName);
	}

	@Override
	public void setBeanNames(String[] names) {
		super.setBeanNames(names);
		beanNames = Arrays.asList(names);
	}
}
