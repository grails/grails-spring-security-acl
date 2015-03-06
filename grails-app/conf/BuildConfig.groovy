grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for the gh-pages branch

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		String springSecurityVersion = '3.2.6.RELEASE'

		compile "org.springframework.security:spring-security-acl:$springSecurityVersion", {
			excludes 'aopalliance', 'commons-logging', 'ehcache', 'fest-assert', 'hsqldb',
			         'jcl-over-slf4j', 'junit', 'logback-classic', 'mockito-core', 'spring-aop',
			         'spring-beans', 'spring-context', 'spring-context-support', 'spring-core',
			         'spring-jdbc', 'spring-security-core', 'spring-test', 'spring-tx'
		}

		// temporary until release plugin v3.1.0 is released
		compile 'commons-io:commons-io:2.1', {
			export = false
		}
	}

	plugins {
		compile ':spring-security-core:2.0-SNAPSHOT'

		compile ':hibernate:3.6.10.17', {
			export = false
		}

		build ':release:3.0.1', ':rest-client-builder:2.0.3', {
			export = false
		}
	}
}
