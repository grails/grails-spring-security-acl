grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for the gh-pages branch

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
		mavenRepo 'http://repo.spring.io/milestone' // TODO remove
	}

	dependencies {
		String springSecurityVersion = '3.2.0.RC1'

		compile "org.springframework.security:spring-security-acl:$springSecurityVersion", {
			excludes 'aopalliance', 'commons-logging', 'ehcache', 'fest-assert', 'hsqldb',
			         'jcl-over-slf4j', 'junit', 'logback-classic', 'mockito-core', 'spring-aop',
			         'spring-beans', 'spring-context', 'spring-context-support', 'spring-core',
			         'spring-jdbc', 'spring-security-core', 'spring-test', 'spring-tx'
		}
	}

	plugins {
		compile ':spring-security-core:2.0-RC2'

		compile ":hibernate:$grailsVersion", {
			export = false
		}

		build ':release:2.2.1', ':rest-client-builder:1.0.3', {
			export = false
		}
	}
}
