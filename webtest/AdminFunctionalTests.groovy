import org.springframework.security.acls.domain.BasePermission

class AdminFunctionalTests extends AbstractSecurityWebTest {

	// admin has admin on all

	void testAdmin() {
		login 'admin', 'admin123'

		checkTags()
		viewAll()
		editReport15()
		deleteReport15()
		grantEdit16()
	}

	private void viewAll() {
		for (int i = 1; i <= 100; i++) {
			get "/report/show/$i" 
			if (i == 11) {
				// deleted by user2
				assertContentContains 'Access Denied'
			}
			else {
				assertContentContains "report$i"
			}
		}
	}

	private void editReport15() {
		get '/report/edit/15'
		assertContentContains 'report15'
		assertContentContains 'Edit Report'

		form {
			name 'report15_new'
			clickButton 'Update'
		}

		get '/report/show/15'
		assertContentContains 'report15_new'
	}

	private void deleteReport15() {
		get '/report/delete/15'
		assertContentContains 'Report 15 deleted'
	}

	private void grantEdit16() {
		get '/report/grant/16'
		assertContentContains 'Grant permission for report16'

		form {
			recipient 'user2'
			permission  BasePermission.READ.mask.toString()
			clickButton 'Grant'
		}

		assertContentContains "Permission $BasePermission.READ.mask granted on Report 16 to user2"

		// login as user2 and verify the grant
		get '/logout'
		login 'user2', 'password2'
		get "/report/show/16" 
		assertContentContains "report16"
	}

	private void checkTags() {
		get '/tagLibTest/test'
		assertContentContains 'test 1 true 1'
		assertContentContains 'test 2 true 1'
		assertContentContains 'test 3 true 1'
		assertContentContains 'test 4 true 1'
		assertContentContains 'test 5 true 1'
		assertContentContains 'test 6 true 1'

		assertContentContains 'test 1 true 13'
		assertContentContains 'test 2 true 13'
		assertContentContains 'test 3 true 13'
		assertContentContains 'test 4 true 13'
		assertContentContains 'test 5 true 13'
		assertContentContains 'test 6 true 13'

		assertContentContains 'test 1 true 80'
		assertContentContains 'test 2 true 80'
		assertContentContains 'test 3 true 80'
		assertContentContains 'test 4 true 80'
		assertContentContains 'test 5 true 80'
		assertContentContains 'test 6 true 80'
	}
}
