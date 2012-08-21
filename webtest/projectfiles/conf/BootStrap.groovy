import com.testacl.Report
import com.testacl.Role
import com.testacl.User
import com.testacl.UserRole

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.DELETE
import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder as SCH

class BootStrap {

	def aclService
	def aclUtilService
	def objectIdentityRetrievalStrategy
	def sessionFactory

	def init = { servletContext ->
		createUsers()

		// Set a user account that will initially own all the created data
		SCH.context.authentication = new UsernamePasswordAuthenticationToken('admin', 'admin123',
				AuthorityUtils.createAuthorityList('ROLE_IGNORED'))

		grantPermissions()

		sessionFactory.currentSession.flush()

		// logout
		SCH.clearContext()
	}

	private void createUsers() {
		def roleAdmin = new Role(authority: 'ROLE_ADMIN').save()
		def roleUser = new Role(authority: 'ROLE_USER').save()

		3.times {
			long id = it + 1
			def user = new User(username: "user$id", enabled: true, password: "password$id").save()
			UserRole.create user, roleUser
		}

		def admin = new User(username: 'admin', enabled: true, password: 'admin123').save()

		UserRole.create admin, roleUser
		UserRole.create admin, roleAdmin, true
	}

	private void grantPermissions() {
		def reports = []
		100.times {
			long id = it + 1
			def report = new Report(name: "report$id").save()
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
