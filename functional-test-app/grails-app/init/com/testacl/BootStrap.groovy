package com.testacl

class BootStrap {

	TestDataService testDataService

	def init = {
		testDataService.reset()
	}
}
