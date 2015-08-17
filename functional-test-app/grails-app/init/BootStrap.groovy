class BootStrap {

	TestDataService testDataService

	def init = {
		testDataService.reset()
	}
}
