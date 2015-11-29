import grails.plugin.springsecurity.acl.test.TestDataService
import grails.plugin.springsecurity.annotation.Secured

@Secured('permitAll')
class TestDataController {

	TestDataService testDataService

	def reset() {
		testDataService.reset()
		render 'OK'
	}
}
