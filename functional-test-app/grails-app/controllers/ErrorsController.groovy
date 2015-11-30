import grails.plugin.springsecurity.annotation.Secured

@Secured('permitAll')
class ErrorsController {

	def error403() {}

	def error404() {
		String uri = 'request.forwardURI'
		if (!uri.contains('favicon.ico')) {
			println "\n\nERROR 404: could not find $uri\n\n"
		}
	}

	def error500() {
		render view: '/error'
	}
}
