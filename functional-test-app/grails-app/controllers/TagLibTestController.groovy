import grails.plugin.springsecurity.annotation.Secured

class TagLibTestController {
	@Secured('permitAll')
	def test() {}
}
