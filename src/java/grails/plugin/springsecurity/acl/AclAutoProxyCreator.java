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
package grails.plugin.springsecurity.acl;

import grails.util.GrailsNameUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;

/**
 * Based on https://github.com/alkemist/grails-aop-reloading-fix/blob/master/src/groovy/grails/plugin/aopreloadingfix/ClassLoaderPerProxyGroovyAwareAspectJAwareAdvisorAutoProxyCreator.groovy
 * and https://github.com/grails-plugins/grails-spring-security-acl/pull/8
 *
 * @author Luke Daley
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 * @author Predrag Knezevic
 */
public class AclAutoProxyCreator extends AbstractAutoProxyCreator implements InitializingBean {

	private static final long serialVersionUID = 1;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected GrailsApplication grailsApplication;
	protected ClassLoader baseLoader;
	protected Collection<String> serviceBeanNames = new ArrayList<String>();

	@SuppressWarnings("unchecked")
	protected final Class<? extends Annotation>[] ANNOTATIONS = new Class[] {
		grails.plugin.springsecurity.annotation.Secured.class,
		org.springframework.security.access.annotation.Secured.class, 
		PreAuthorize.class,
		PreFilter.class, 
		PostAuthorize.class, 
		PostFilter.class};

	@Override
	protected Object[] getAdvicesAndAdvisorsForBean(final Class<?> beanClass, final String beanName, final TargetSource customTargetSource) throws BeansException {
		if (serviceBeanNames.contains(beanName) || shouldProxy(beanClass, beanName)) {
			return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
		}
		return DO_NOT_PROXY;
	}

	protected boolean beanIsAnnotated(final Class<?> c) {
		for (Class<? extends Annotation> annotation: ANNOTATIONS) {
			if (c.isAnnotationPresent(annotation)) {
				return true;
			}

			for (Method method : c.getMethods()) {
				if (method.isAnnotationPresent(annotation)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
		return serviceBeanNames.contains(beanName) || super.shouldProxyTargetClass(beanClass, beanName);
	}

	@Override
	protected Object getCacheKey(Class<?> beanClass, String beanName) {
		return beanClass.hashCode() + "_" + beanName;
	}

	protected boolean shouldProxy(final Class<?> c, final String beanName) {

		if (grailsApplication.isArtefactOfType(ControllerArtefactHandler.TYPE, c)) {
			// pre and post annotations don't make sense, and @Secured is handled by url checks
			return false;
		}

		boolean hasSpringSecurityACL = GrailsClassUtils.isStaticProperty(c, "springSecurityACL");
		if (hasSpringSecurityACL || beanIsAnnotated(c)) {
			if (log.isDebugEnabled()) log.debug("Secure '{0}' instances of {1}", new Object[] { beanName, c.getName() });
			return true;
		}
		return false; 
	}

	@Override
	protected Object createProxy(Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {
		try {
			setProxyClassLoader(new GrailsAwareClassLoader(baseLoader, null, false));
			return super.createProxy(beanClass, beanName, specificInterceptors, targetSource);
		}
		finally {
			setProxyClassLoader(baseLoader);
		}
	}

	@Override
	public void setBeanClassLoader(final ClassLoader classLoader) {
		super.setBeanClassLoader(classLoader);
		baseLoader = classLoader;
	}

	public void afterPropertiesSet() throws Exception {
		for (GrailsClass serviceClass : grailsApplication.getArtefacts(ServiceArtefactHandler.TYPE)) {
			String beanName = GrailsNameUtils.getPropertyNameRepresentation(serviceClass.getClazz().getName());
			if (shouldProxy(serviceClass.getClazz(), beanName)) {
				serviceBeanNames.add(beanName);
			}
		}
	}

	/**
	 * Dependency injection for the application.
	 * @param application the application
	 */
	public void setGrailsApplication(GrailsApplication application) {
		grailsApplication = application;
	}
}
