package com.testacl

import grails.plugin.springsecurity.annotation.Secured
import groovy.transform.CompileStatic

@CompileStatic
@Secured('permitAll')
class ErrorsController {

	def error403() {
        // Line necessary due to: https://github.com/grails/grails-core/issues/10582
		[view: 'error403']
	}

	def error404() {
		String uri = 'request.forwardURI'
		if (!uri.contains('favicon.ico')) {
			println "\n\nERROR 404: could not find $uri\n\n"
		}
		[view: 'error404']
	}

	def error500() {
		render view: '/error'
	}
}
