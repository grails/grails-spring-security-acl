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
		String springSecurityVersion = '3.2.5.RELEASE'
	
		compile "org.springframework.security:spring-security-acl:$springSecurityVersion", {
			excludes 'aopalliance', 'commons-logging', 'ehcache', 'fest-assert', 'hsqldb',
			         'jcl-over-slf4j', 'junit', 'logback-classic', 'mockito-core', 
			         'spring-beans', 'spring-context', 'spring-context-support', 'spring-core', 'spring-orm',
			         'spring-jdbc', 'spring-security-core', 'spring-test', 'spring-tx'
		}
		
		provided 'org.springframework:spring-expression:4.0.6.RELEASE'
		
		compile 'org.apache.commons:commons-io:1.3.2'
	}

	plugins {
		compile ':spring-security-core:2.0-RC4'
		
		compile ":hibernate4:4.3.5.5", {
			export = false
		}

		build ':release:3.0.1', ':rest-client-builder:1.0.3', {
			export = false
		}
	}
}
