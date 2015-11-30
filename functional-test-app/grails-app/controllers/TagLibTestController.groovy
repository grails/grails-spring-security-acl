import com.testacl.Report
import grails.plugin.springsecurity.annotation.Secured

class TagLibTestController {
	@Secured('permitAll')
	def test() {
		[reportIdsAndNumbers: [1, 13, 80].collect { [id: Report.findByNumber(it).id, number: it] }]
	}
}
