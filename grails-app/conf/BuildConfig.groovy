grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for the gh-pages branch
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile('org.springframework.security:spring-security-acl:3.0.7.RELEASE') {
			excludes 'spring-security-core', 'spring-context-support', 'spring-jdbc',
			         'spring-test', 'ehcache', 'hsqldb', 'postgresql', 'junit',
			         'mockito-core', 'jmock-junit4'
		}
	}

	plugins {
		compile ':spring-security-core:1.2.7.3'

		compile(":hibernate:$grailsVersion") {
			export = false
		}

		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}
	}
}
