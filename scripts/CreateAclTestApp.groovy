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
 * Creates a test application for functional tests.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */

includeTargets << grailsScript('_GrailsBootstrap')

functionalTestPluginVersion = '1.2.7'
appName = null
grailsHome = null
dotGrails = null
projectDir = null
pluginVersion = null
pluginZip = null
testprojectRoot = null

target(createAclTestApps: 'Creates ACL test app') {
	init()
	createApp()
	installPlugins()
	runQuickstart()
	createProjectFiles()
}

private void init() {

	def configFile = new File(basedir, 'testapps.config.properties')
	if (!configFile.exists()) {
		error "$configFile.path not found"
	}
	def props = new Properties()
	props.load new FileInputStream(configFile)
	appName = props.appName
	grailsHome = props.grailsHome
	dotGrails = props.dotGrails
	projectDir = props.projectDir
	pluginVersion = props.pluginVersion

	pluginZip = new File(basedir, "grails-spring-security-acl-${pluginVersion}.zip")
	if (!pluginZip.exists()) {
		error "plugin $pluginZip.absolutePath not found"
	}

	if (!new File(grailsHome).exists()) {
		error "Grails home $grailsHome not found"
	}

	testprojectRoot = "$projectDir/$appName"
}

private void createApp() {

	ant.mkdir dir: projectDir

	deleteDir testprojectRoot
	deleteDir "$dotGrails/projects/$appName"

	callGrails(grailsHome, projectDir, 'dev', 'create-app') {
		ant.arg value: appName
	}
}

private void installPlugins() {

	// install plugins in local dir to make optional STS setup easier
	// also configure the functional tests to run in order
	new File("$testprojectRoot/grails-app/conf/BuildConfig.groovy").withWriterAppend {
		it.writeLine 'grails.project.plugins.dir = "plugins"'
		it.writeLine 'grails.testing.patterns = ["User1Functional", "User2Functional", "AdminFunctional"]'
	}

	ant.mkdir dir: "${testprojectRoot}/plugins"
	callGrails(grailsHome, testprojectRoot, 'dev', 'install-plugin') {
		ant.arg value: "functional-test ${functionalTestPluginVersion}"
	}
	callGrails(grailsHome, testprojectRoot, 'dev', 'install-plugin') {
		ant.arg value: pluginZip.absolutePath
	}
}

private void runQuickstart() {
	callGrails(grailsHome, testprojectRoot, 'dev', 's2-quickstart') {
		ant.arg value: 'com.testacl'
		ant.arg value: 'User'
		ant.arg value: 'Role'
	}
}

private void createProjectFiles() {
	String source = "$basedir/webtest/projectfiles"
	ant.copy file: "$source/resources.groovy", todir: "$testprojectRoot/grails-app/conf/spring", overwrite: true
	ant.copy file: "$source/classpath", tofile: "$testprojectRoot/.classpath", overwrite: true
	ant.copy file: "$source/Report.groovy", todir: "$testprojectRoot/grails-app/domain/com/testacl"

	for (type in ['conf', 'controllers', 'services', 'views']) {
		ant.copy(todir: "$testprojectRoot/grails-app/$type", overwrite: true) {
			fileset dir: "$source/$type"
		}
	}

	ant.copy(todir: "$testprojectRoot/test/functional", overwrite: true) {
		fileset dir: "$basedir/webtest", includes: "*Test*.groovy"
	}

	new File("$testprojectRoot/grails-app/conf/Config.groovy").withWriterAppend {
		it.writeLine "grails.plugins.springsecurity.roleHierarchy = 'ROLE_ADMIN > ROLE_USER'"
	}
}

private void deleteDir(String path) {
	if (new File(path).exists()) {
		String code = "confirm.delete.$path"
		ant.input message: "$path exists, ok to delete?", addproperty: code, validargs: 'y,n'
		def result = ant.antProject.properties[code]
		if (!'y'.equalsIgnoreCase(result)) {
			ant.echo "\nNot deleting $path"
			exit 1
		}
	}

	ant.delete dir: path
}

private void error(String message) {
	ant.echo "\nERROR: $message"
	exit 1
}

private void callGrails(String grailsHome, String dir, String env, String action, extraArgs = null) {
	ant.exec(executable: "${grailsHome}/bin/grails", dir: dir, failonerror: 'true') {
		ant.env key: 'GRAILS_HOME', value: grailsHome
		ant.arg value: env
		ant.arg value: action
		extraArgs?.call()
	}
}

setDefaultTarget 'createAclTestApps'
