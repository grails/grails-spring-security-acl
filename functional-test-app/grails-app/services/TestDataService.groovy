import com.testacl.Report
import com.testacl.Role
import com.testacl.User
import com.testacl.UserRole
import grails.plugin.springsecurity.acl.AclClass
import grails.plugin.springsecurity.acl.AclEntry
import grails.plugin.springsecurity.acl.AclObjectIdentity
import grails.plugin.springsecurity.acl.AclService
import grails.plugin.springsecurity.acl.AclSid
import grails.plugin.springsecurity.acl.AclUtilService
import grails.transaction.Transactional
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder as SCH

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

@Transactional
class TestDataService {

	AclService aclService
	AclUtilService aclUtilService
	ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy

	void reset() {
		deleteAll()
		createData()
	}

	void deleteAll() {
		[AclEntry, AclObjectIdentity, AclSid, AclClass, UserRole, User, Role, Report].each {
			it.list()*.delete()
		}
	}

	void createData() {
		createUsers()

		// Set a user account that will initially own all the created data
		SCH.context.authentication = new UsernamePasswordAuthenticationToken('admin', 'admin123',
				[new SimpleGrantedAuthority('ROLE_IGNORED')])

		grantPermissions()

		transactionStatus.flush()

		// logout
		SCH.clearContext()
	}

	private void createUsers() {
		def roleAdmin = new Role('ROLE_ADMIN').save(failOnError: true)
		def roleUser = new Role('ROLE_USER').save(failOnError: true)

		3.times {
			long id = it + 1
			def user = new User("user$id", "password$id").save(failOnError: true)
			UserRole.create user, roleUser
		}

		def admin = new User('admin', 'admin123').save(failOnError: true)

		UserRole.create admin, roleUser
		UserRole.create admin, roleAdmin
	}

	private void grantPermissions() {
		def reports = []
		100.times {
			int number = it + 1
			def report = new Report("report$number", number).save(failOnError: true)
			reports << report
			aclService.createAcl objectIdentityRetrievalStrategy.getObjectIdentity(report)
		}

		// grant user 1 admin on 11,12 and read on 1-67
		aclUtilService.addPermission reports[10], 'user1', ADMINISTRATION
		aclUtilService.addPermission reports[11], 'user1', ADMINISTRATION
		67.times {
			aclUtilService.addPermission reports[it], 'user1', READ
		}

		// grant user 2 read on 1-5, write on 5
		5.times {
			aclUtilService.addPermission reports[it], 'user2', READ
		}
		aclUtilService.addPermission reports[4], 'user2', WRITE

		// user 3 has no grants

		// grant admin read and admin on all
		for (report in reports) {
			aclUtilService.addPermission report, 'admin', READ
			aclUtilService.addPermission report, 'admin', ADMINISTRATION
		}

		// grant user 1 ownership on 1,2 to allow the user to grant
		aclUtilService.changeOwner reports[0], 'user1'
		aclUtilService.changeOwner reports[1], 'user1'
	}
}
