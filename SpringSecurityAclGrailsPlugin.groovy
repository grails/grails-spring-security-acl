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
import java.lang.reflect.Method

import grails.plugins.springsecurity.Secured as GrailsSecured
import grails.plugins.springsecurity.acl.AclVoter
import grails.plugins.springsecurity.acl.AclVoters
import grails.util.GrailsNameUtils

import org.apache.log4j.Logger

import org.codehaus.groovy.grails.commons.ApplicationHolder as AH
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.acl.ClassLoaderPerProxyBeanNameAutoProxyCreator
import org.codehaus.groovy.grails.plugins.springsecurity.acl.GormAclLookupStrategy
import org.codehaus.groovy.grails.plugins.springsecurity.acl.GormObjectIdentityRetrievalStrategy
import org.codehaus.groovy.grails.plugins.springsecurity.acl.GroovyAwareAclVoter
import org.codehaus.groovy.grails.plugins.springsecurity.acl.NullAclAuditLogger
import org.codehaus.groovy.grails.plugins.springsecurity.acl.ProxyAwareDelegatingMethodSecurityMetadataSource
import org.codehaus.groovy.grails.plugins.springsecurity.acl.ProxyAwareParameterNameDiscoverer
import org.codehaus.groovy.grails.plugins.springsecurity.acl.SecuredAnnotationSecurityMetadataSource as GrailsSecuredAnnotationSecurityMetadataSource
import org.codehaus.groovy.grails.plugins.springsecurity.acl.ServiceStaticMethodSecurityMetadataSource

import org.springframework.cache.ehcache.EhCacheFactoryBean
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.security.access.ConfigAttribute
import org.springframework.security.access.SecurityConfig
import org.springframework.security.access.annotation.Secured as SpringSecured
import org.springframework.security.access.annotation.SecuredAnnotationSecurityMetadataSource as SpringSecuredAnnotationSecurityMetadataSource
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.ExpressionBasedAnnotationAttributeFactory
import org.springframework.security.access.expression.method.ExpressionBasedPostInvocationAdvice
import org.springframework.security.access.expression.method.ExpressionBasedPreInvocationAdvice
import org.springframework.security.access.intercept.AfterInvocationProviderManager
import org.springframework.security.access.intercept.RunAsImplAuthenticationProvider
import org.springframework.security.access.intercept.RunAsManagerImpl
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor
import org.springframework.security.access.intercept.aopalliance.MethodSecurityMetadataSourceAdvisor
import org.springframework.security.access.method.MapBasedMethodSecurityMetadataSource
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PostInvocationAdviceProvider
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PreFilter
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdviceVoter
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource
import org.springframework.security.access.vote.AffirmativeBased
import org.springframework.security.acls.AclEntryVoter
import org.springframework.security.acls.AclPermissionEvaluator
import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider
import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.DefaultPermissionFactory
import org.springframework.security.acls.domain.EhCacheBasedAclCache
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.GrantedAuthorityImpl

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class SpringSecurityAclGrailsPlugin {

	String version = '1.1'
	String grailsVersion = '1.3 > *'
	Map dependsOn = [springSecurityCore: '1.0 > *']
	List pluginExcludes = [
		'docs/**',
		'src/docs/**',
		'grails-app/services/**/Test*Service.groovy',
		'scripts/CreateAclTestApp.groovy'
	]

	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String title = 'ACL support for the Spring Security plugin.'
	String description = 'ACL support for the Spring Security plugin.'

	String documentation = 'http://grails.org/plugin/spring-security-acl'

	def doWithSpring = {

		def conf = SpringSecurityUtils.securityConfig
		if (!conf || !conf.active) {
			return
		}

		SpringSecurityUtils.loadSecondaryConfig 'DefaultAclSecurityConfig'
		// have to get again after overlaying DefaultAclSecurityConfig
		conf = SpringSecurityUtils.securityConfig

		if (!conf.acl.active) {
			return
		}

		println 'Configuring Spring Security ACL ...'

		if (conf.useRunAs) {
			SpringSecurityUtils.registerProvider 'runAsAuthenticationProvider'
		}

		Map voterConfig = buildVoterConfig(conf, application)
		debug "voterConfig: $voterConfig"

		// core beans
		configureCoreBeans.delegate = delegate
		configureCoreBeans conf

		// expression support
		configureExpressionBeans.delegate = delegate
		configureExpressionBeans conf

		// secured services
		configureSecuredServices.delegate = delegate
		configureSecuredServices conf, application

		// MetadataSource
		configureSecurityMetadataSource.delegate = delegate
		configureSecurityMetadataSource conf, voterConfig, application
	}

	def doWithApplicationContext = { ctx ->

		def conf = SpringSecurityUtils.securityConfig
		if (!conf || !conf.active || !conf.acl.active) {
			return
		}

		ctx.aclSecurityMetadataSource.methodSecurityMetadataSources = [
			ctx.prePostAnnotationSecurityMetadataSource,
			ctx.springSecuredAnnotationSecurityMetadataSource,
			ctx.grailsSecuredAnnotationSecurityMetadataSource,
			ctx.serviceStaticMethodSecurityMetadataSource
		]
	}

	private configureCoreBeans = { conf ->

		sidRetrievalStrategy(SidRetrievalStrategyImpl, ref('roleHierarchy'))

		objectIdentityRetrievalStrategy(GormObjectIdentityRetrievalStrategy)

		// acl cache
		aclCacheManager(EhCacheManagerFactoryBean)
		ehcacheAclCache(EhCacheFactoryBean) {
			cacheManager = ref('aclCacheManager')
			cacheName = 'aclCache'
		}
		aclCache(EhCacheBasedAclCache, ehcacheAclCache)

		aclAuthorizationStrategy(AclAuthorizationStrategyImpl,
				AuthorityUtils.createAuthorityList(
						conf.acl.authority.changeOwnership,
						conf.acl.authority.modifyAuditingDetails,
						conf.acl.authority.changeAclDetails) as GrantedAuthority[]) {
			sidRetrievalStrategy = ref('sidRetrievalStrategy')
		}

		aclAuditLogger(NullAclAuditLogger)

		aclPermissionFactory(DefaultPermissionFactory)

		aclLookupStrategy(GormAclLookupStrategy) {
			aclAuthorizationStrategy = ref('aclAuthorizationStrategy')
			aclCache = ref('aclCache')
			permissionFactory = ref('aclPermissionFactory')
			auditLogger = ref('aclAuditLogger')
		}
	}

	private configureExpressionBeans = { conf ->

		parameterNameDiscoverer(ProxyAwareParameterNameDiscoverer)

		permissionEvaluator(AclPermissionEvaluator, ref('aclService')) {
			objectIdentityRetrievalStrategy = ref('objectIdentityRetrievalStrategy')
			objectIdentityGenerator = ref('objectIdentityRetrievalStrategy')
			sidRetrievalStrategy = ref('sidRetrievalStrategy') 
			permissionFactory = ref('aclPermissionFactory')
		}

		expressionParser(SpelExpressionParser)

		expressionHandler(DefaultMethodSecurityExpressionHandler) {
			parameterNameDiscoverer = ref('parameterNameDiscoverer')
			permissionEvaluator = ref('permissionEvaluator')
			roleHierarchy = ref('roleHierarchy')
			trustResolver = ref('authenticationTrustResolver')
		}
	}

	private configureSecuredServices = { conf, application ->

		debug 'configuring secured services'

		if (conf.useRunAs) {
			runAsManager(RunAsManagerImpl) {
				key = conf.runAs.key
			}

			runAsAuthenticationProvider(RunAsImplAuthenticationProvider) {
				key = conf.runAs.key
			}
		}

		def serviceNames = []
		for (serviceClass in application.serviceClasses) {
			boolean hasSpringSecurityACL = GCU.isStaticProperty(serviceClass.clazz, 'springSecurityACL')
			if (hasSpringSecurityACL || serviceIsAnnotated(serviceClass.clazz)) {
				serviceNames << GrailsNameUtils.getPropertyNameRepresentation(serviceClass.clazz.name)
			}
		}

		if (serviceNames) {
			securedServicesInterceptor(ClassLoaderPerProxyBeanNameAutoProxyCreator) {
				proxyTargetClass = true
				beanNames = serviceNames
				interceptorNames = ['methodSecurityInterceptor']
			}
		}
	}

	private boolean serviceIsAnnotated(Class clazz) {
		for (annotation in [GrailsSecured, SpringSecured, PreAuthorize,
		                    PreFilter, PostAuthorize, PostFilter]) {
			if (serviceIsAnnotated(clazz, annotation)) {
				return true
			}
		}
		false
	}

	private boolean serviceIsAnnotated(Class clazz, Class annotation) {
		if (clazz.isAnnotationPresent(annotation)) {
			return true
		}

		for (Method method in clazz.methods) {
			if (method.isAnnotationPresent(annotation)) {
				return true
			}
		}

		false
	}

	private configureSecurityMetadataSource = { conf, voterConfig, application ->

		// create decision voters

		groovyAwareAclVoter(GroovyAwareAclVoter)

		def aclAccessDecisionManagerDecisionVoters = [ref('roleVoter'),
		                                              ref('authenticatedVoter'),
		                                              ref('preInvocationVoter'),
		                                              ref('groovyAwareAclVoter')]

		voterConfig.each { beanName, voterData ->
			"$beanName"(AclEntryVoter, ref('aclService'), voterData.configAttribute, voterData.permissions) {
				processDomainObjectClass = voterData.domainObjectClass
				internalMethod = voterData.internalMethod
				objectIdentityRetrievalStrategy = ref('objectIdentityRetrievalStrategy')
				sidRetrievalStrategy = ref('sidRetrievalStrategy')
			}
			aclAccessDecisionManagerDecisionVoters << ref(beanName)
			debug "created AclEntryVoter $beanName for domain class $voterData.domainObjectClass.name with configAttribute $voterData.configAttribute and permissions $voterData.permissions"
		}

		aclAccessDecisionManager(AffirmativeBased) {
			allowIfAllAbstainDecisions = false
			decisionVoters = aclAccessDecisionManagerDecisionVoters
		}

		// processes AFTER_ACL_COLLECTION_READ configuration settings;
		// filters out records you don't have access to
		afterAclCollectionRead(AclEntryAfterInvocationCollectionFilteringProvider, ref('aclService'),
				[BasePermission.READ]) {
			objectIdentityRetrievalStrategy = ref('objectIdentityRetrievalStrategy')
			sidRetrievalStrategy = ref('sidRetrievalStrategy')
		}

		// processes AFTER_ACL_READ configuration settings;
		// determines access to single instances returned
		afterAclRead(AclEntryAfterInvocationProvider, ref('aclService'),
				[BasePermission.READ]) {
			objectIdentityRetrievalStrategy = ref('objectIdentityRetrievalStrategy')
			sidRetrievalStrategy = ref('sidRetrievalStrategy')
		}

		preInvocationAdvice(ExpressionBasedPreInvocationAdvice) {
			expressionHandler = ref('expressionHandler')
		}

		preInvocationVoter(PreInvocationAuthorizationAdviceVoter, ref('preInvocationAdvice'))

		postInvocationAdvice(ExpressionBasedPostInvocationAdvice, ref('expressionHandler'))

		annotationInvocationFactory(ExpressionBasedAnnotationAttributeFactory, ref('expressionHandler'))

		Map<String, List<String>> classConfigNameMap = [:]
		Map<String, Map<String, List<String>>> methodConfigNameMap = [:]
		findConfigNames classConfigNameMap, methodConfigNameMap, application

		serviceStaticMethodSecurityMetadataSource(ServiceStaticMethodSecurityMetadataSource) {
			classConfigNames = classConfigNameMap
			methodConfigNames = methodConfigNameMap
		}

		prePostAnnotationSecurityMetadataSource(PrePostAnnotationSecurityMetadataSource, ref('annotationInvocationFactory'))
		grailsSecuredAnnotationSecurityMetadataSource(GrailsSecuredAnnotationSecurityMetadataSource) {
			serviceClassNames = AH.application.serviceClasses*.clazz.name
		}
		springSecuredAnnotationSecurityMetadataSource(SpringSecuredAnnotationSecurityMetadataSource)

		def metadataSources = [ref('prePostAnnotationSecurityMetadataSource'),
		                       ref('springSecuredAnnotationSecurityMetadataSource'),
		                       ref('serviceStaticMethodSecurityMetadataSource')]
		aclSecurityMetadataSource(ProxyAwareDelegatingMethodSecurityMetadataSource) {
			methodSecurityMetadataSources = metadataSources
		}

		postInvocationProvider(PostInvocationAdviceProvider, ref('postInvocationAdvice'))
		afterInvocationManager(AfterInvocationProviderManager) {
			providers = [ref('postInvocationProvider'),
			             ref('afterAclRead'),
			             ref('afterAclCollectionRead')]
		}

		methodSecurityInterceptor(MethodSecurityInterceptor) {
			accessDecisionManager = ref('aclAccessDecisionManager')
			authenticationManager = ref('authenticationManager')
			afterInvocationManager = ref('afterInvocationManager')
			securityMetadataSource = ref('aclSecurityMetadataSource')
			runAsManager = ref('runAsManager')
			validateConfigAttributes = false
		}

//		methodSecurityMetadataSourceAdvisor(MethodSecurityMetadataSourceAdvisor,
//				'methodSecurityInterceptor', ref('aclSecurityMetadataSource'), 'aclSecurityMetadataSource')
	}

	private void findConfigNames(Map<String, List<String>> classConfigNames,
			Map<String, Map<String, List<String>>> methodConfigNames, application) {

		/*
		Look for configurations like this in services:

		static springSecurityACL = [
			getReportName: ['ROLE_USER', 'ROLE_ADMIN'],
			getAllReports: ['ROLE_USER', 'AFTER_ACL_COLLECTION_READ'],
			getReport: ['ROLE_USER', 'AFTER_ACL_READ'],
			updateReport: ['ACL_REPORT_WRITE'],
			deleteReport: ['ACL_REPORT_DELETE']
		]
		*/

		for (serviceClass in application.serviceClasses) {
			methodConfigNames[serviceClass.clazz.name] = [:]
		}

		for (serviceClass in application.serviceClasses) {
			if (!GCU.isStaticProperty(serviceClass.clazz, 'springSecurityACL')) {
				continue
			}

			String className = serviceClass.clazz.name
			def springSecurityACL = serviceClass.clazz.springSecurityACL
			springSecurityACL.each { methodName, configNames ->
				if ('*'.equals(methodName)) {
					classConfigNames[className] = configNames
				}
				else {
					methodConfigNames[className][methodName] = configNames
				}
			}
		}
	}

	private Map buildVoterConfig(conf, application) {

		/*
		Look for annotations like this in domain classes:

		@AclVoters([
			@AclVoter(name='aclReportWriteVoter',
			          configAttribute='ACL_REPORT_WRITE',
			          permissions=['ADMINISTRATION', 'WRITE']),
			@AclVoter(name='aclReportDeleteVoter',
			          configAttribute='ACL_REPORT_DELETE',
			          permissions=['ADMINISTRATION', 'DELETE'])
		])

		In addition you can declare a config attribute 'acl.voters':

		grails.plugins.springsecurity.acl.voters = [
			aclReportWriteVoter: [
				domainObjectClass: Report,
				configAttribute: 'ACL_REPORT_WRITE',
				permissions: [BasePermission.ADMINISTRATION,
				              BasePermission.WRITE]
			],
	
			aclReportDeleteVoter: [
				domainObjectClass: Report,
				configAttribute: 'ACL_REPORT_DELETE',
				permissions: [BasePermission.ADMINISTRATION,
				              BasePermission.DELETE]
			]
		]
		*/

		Map config = conf.acl.voters.clone()

		for (dc in application.domainClasses) {
			for (annotation in findAclVoterAnnotations(dc.clazz)) {
				def permissions = []
				for (String permissionName in annotation.permissions()) {
					permissions << BasePermission."$permissionName"
				}
				config[annotation.name()] = [configAttribute: annotation.configAttribute(),
				                             domainObjectClass: dc.clazz,
				                             permissions: permissions]
			}
		}

		config
	}

	/**
	 * Look for @AclVoter annotations in the specified domain class.
	 */
	private List<AclVoter> findAclVoterAnnotations(Class<?> domainClass) {
		List<AclVoter> annotations = []

		AclVoter aclVoterAnnotation = domainClass.getAnnotation(AclVoter)
		if (aclVoterAnnotation) {
			annotations << aclVoterAnnotation
		}

		AclVoters aclVotersAnnotation = domainClass.getAnnotation(AclVoters)
		if (aclVotersAnnotation) {
			annotations.addAll aclVotersAnnotation.value() as List
		}

		annotations
	}

	private void debug(message) {
//		log.debug message
//		println message
	}
}
