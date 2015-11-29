import grails.plugin.springsecurity.acl.test.TestDataService

class BootStrap {

	TestDataService testDataService

	def init = {
		testDataService.reset()
	}
}
