import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclClass
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.acls.model.Permission;

import com.testacl.Report

import grails.plugins.springsecurity.Secured

@Secured(['ROLE_USER'])
class ReportController {

	static defaultAction = 'list'

	def reportService

	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[reportInstanceList: reportService.list(params),
		 reportInstanceTotal: reportService.count()]
	}

	def create = {
		[reportInstance: new Report(params)]
	}

	def save = {
		def report = reportService.create(params.name)
		if (!renderWithErrors('create', report)) {
			redirectShow "Report $report.id created", report.id
		}
	}

	def show = {
		def report = findInstance()
		if (!report) return

		[reportInstance: report]
	}

	def edit = {
		def report = findInstance()
		if (!report) return

		[reportInstance: report]
	}

	def update = {
		def report = findInstance()
		if (!report) return

		reportService.update report, params.name
		if (!renderWithErrors('edit', report)) {
			redirectShow "Report $report.id updated", report.id
		}
	}

	def delete = {
		def report = findInstance()
		if (!report) return

		try {
			reportService.delete report
			flash.message = "Report $params.id deleted"
			redirect action: list
		}
		catch (DataIntegrityViolationException e) {
			redirectShow "Report $params.id could not be deleted", params.id
		}
	}

	def grant = {

		def report = findInstance()
		if (!report) return

		if (!request.post) {
			return [reportInstance: report]
		}

		reportService.addPermission report, params.recipient, params.int('permission')

		redirectShow "Permission $params.permission granted on Report $report.id to $params.recipient", report.id
	}

	def dump = {

		def html = new StringBuilder()

		html << "<br/>${AclClass.count()} AclClass:<br/>"
		AclClass.list().each {
			html << "&nbsp;&nbsp;&nbsp;ID: $it.id, class name: $it.className<br/>"
		}

		html << "<br/>${AclSid.count()} AclSid:<br/>"
		AclSid.list().each {
			html << "&nbsp;&nbsp;&nbsp;ID: $it.id, SID: $it.sid : principal: $it.principal<br/>"
		}

		html << "<br/>${AclObjectIdentity.count()} AclObjectIdentity:<br/>"
		AclObjectIdentity.list().each {
			html << "&nbsp;&nbsp;&nbsp;ID: $it.id, objectId: $it.objectId, aclClass: $it.aclClass.id($it.aclClass.className), owner: $it.owner.id<br/>"
		}

		html << "<br/>${AclEntry.count()} AclEntry:<br/>"
		AclEntry.list().each {
			html << "&nbsp;&nbsp;&nbsp;ID: $it.id, aclObjectIdentity: $it.aclObjectIdentity.id, order: $it.aceOrder, sid: $it.sid.id($it.sid.sid), mask: $it.mask<br/>"
		}

		render html.toString()
	}

	private Report findInstance() {
		def report = reportService.get(params.long('id'))
		if (!report) {
			flash.message = "Report not found with id $params.id"
			redirect action: list
		}
		report
	}

	private void redirectShow(String message, id) {
		flash.message = message
		redirect action: show, id: id
	}

	private boolean renderWithErrors(String view, Report report) {
		if (report.hasErrors()) {
			render view: view, model: [reportInstance: report]
			return true
		}
		false
	}
}
