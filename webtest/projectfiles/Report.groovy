package com.testacl

import grails.plugins.springsecurity.acl.AclVoter
import grails.plugins.springsecurity.acl.AclVoters

@AclVoters([
	@AclVoter(name='aclReportWriteVoter',
	          configAttribute='ACL_REPORT_WRITE',
	          permissions=['ADMINISTRATION', 'WRITE']),
	@AclVoter(name='aclReportDeleteVoter',
	          configAttribute='ACL_REPORT_DELETE',
	          permissions=['ADMINISTRATION', 'DELETE'])
])
class Report {
	String name
}
