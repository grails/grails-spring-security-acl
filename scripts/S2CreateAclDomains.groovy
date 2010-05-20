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

/**
 * Creates ACL domain classes in the project directory. The package and names cannot be
 * specified since plugin classes depend on them. But they should be in the project's
 * grails-app/domain folder to allow customization such as Hibernate 2nd-level caching.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */

includeTargets << grailsScript('_GrailsBootstrap')

target(s2CreateAclDomains: 'Creates ACL Domain classes for Spring Security ACL plugin') {
	depends(checkVersion, configureProxy, classpath)

	String appDir = "$basedir/grails-app"
	String destFolder = "$appDir/domain/org/codehaus/groovy/grails/plugins/springsecurity/acl"
	String templateDir = "$springSecurityAclPluginDir/src/templates"

	ant.mkdir dir: destFolder

	['AclClass', 'AclEntry', 'AclObjectIdentity', 'AclSid'].each { name ->
		ant.copy file: "$templateDir/_${name}.groovy", tofile: "$destFolder/${name}.groovy"
	}

	ant.echo """
	**********************************************
	* Your ACL domain classes have been created. *
	**********************************************
"""
}

setDefaultTarget 's2CreateAclDomains'
