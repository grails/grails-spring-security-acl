/* Copyright 2009-2012 SpringSource.
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
 * Creates test applications for functional tests.
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
deleteAll = false

target(createAclTestApps: 'Creates ACL test apps') {

	def configFile = new File(basedir, 'testapps.config.groovy')
	if (!configFile.exists()) {
		error "$configFile.path not found"
	}

	new ConfigSlurper().parse(configFile.text).each { name, config ->
		printMessage "\nCreating app based on configuration $name: ${config.flatten()}\n"
		init name, config
		createApp()
		installPlugins()
		runQuickstart()
		createProjectFiles()
	}
}

private void init(String name, config) {

	pluginVersion = config.pluginVersion
	if (!pluginVersion) {
		error "pluginVersion wasn't specified for config '$name'"
	}

	pluginZip = new File(basedir, "grails-spring-security-acl-${pluginVersion}.zip")
	if (!pluginZip.exists()) {
		error "plugin $pluginZip.absolutePath not found"
	}

	grailsHome = config.grailsHome
	if (!new File(grailsHome).exists()) {
		error "Grails home $grailsHome not found"
	}

	projectDir = config.projectDir
	appName = 'spring-security-acl-test-' + name
	testprojectRoot = "$projectDir/$appName"
	dotGrails = config.dotGrails + '/' + grailsVersion
}

private void createApp() {

	ant.mkdir dir: projectDir

	deleteDir testprojectRoot
	deleteDir "$dotGrails/projects/$appName"

	callGrails grailsHome, projectDir, 'dev', 'create-app', [appName]
}

private void installPlugins() {

	File buildConfig = new File(testprojectRoot, 'grails-app/conf/BuildConfig.groovy')
	String contents = buildConfig.text

	contents = contents.replace('grails.project.class.dir = "target/classes"', "grails.project.work.dir = 'target'")
	contents = contents.replace('grails.project.test.class.dir = "target/test-classes"', '')
	contents = contents.replace('grails.project.test.reports.dir = "target/test-reports"', '')

	// configure the functional tests to run in order
	contents += '\ngrails.testing.patterns = ["User1Functional", "User2Functional", "AdminFunctional"]\n'

	buildConfig.withWriter { it.writeLine contents }

	callGrails grailsHome, testprojectRoot, 'dev', 'install-plugin', ["functional-test ${functionalTestPluginVersion}"]

	callGrails grailsHome, testprojectRoot, 'dev', 'install-plugin', [pluginZip.absolutePath]

	callGrails grailsHome, testprojectRoot, 'dev', 'compile'
}

private void runQuickstart() {
	callGrails grailsHome, testprojectRoot, 'dev', 's2-quickstart', ['com.testacl', 'User', 'Role']
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
	if (new File(path).exists() && !deleteAll) {
		String code = "confirm.delete.$path"
		ant.input message: "$path exists, ok to delete?", addproperty: code, validargs: 'y,n,a'
		def result = ant.antProject.properties[code]
		if ('a'.equalsIgnoreCase(result)) {
			deleteAll = true
		}
		else if (!'y'.equalsIgnoreCase(result)) {
			printMessage "\nNot deleting $path"
			exit 1
		}
	}

	ant.delete dir: path
}

private void error(String message) {
	errorMessage "\nERROR: $message"
	exit 1
}

private void callGrails(String grailsHome, String dir, String env, String action, List extraArgs = null) {

	String resultproperty = 'exitCode' + System.currentTimeMillis()
	String outputproperty = 'execOutput' + System.currentTimeMillis()

	println "Running 'grails $env $action ${extraArgs?.join(' ') ?: ''}'"

	ant.exec(executable: "${grailsHome}/bin/grails", dir: dir, failonerror: false,
				resultproperty: resultproperty, outputproperty: outputproperty) {
		ant.env key: 'GRAILS_HOME', value: grailsHome
		ant.arg value: env
		ant.arg value: action
		extraArgs.each { ant.arg value: it }
	}

	println ant.project.getProperty(outputproperty)

	int exitCode = ant.project.getProperty(resultproperty) as Integer
	if (exitCode) {
		exit exitCode
	}
}

printMessage = { String message -> event('StatusUpdate', [message]) }
errorMessage = { String message -> event('StatusError', [message]) }

setDefaultTarget 'createAclTestApps'
