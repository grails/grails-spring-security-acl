import org.springframework.security.acls.domain.BasePermission

class User1FunctionalTests extends AbstractSecurityWebTest {

	// user1 has admin on 11,12 and read on 1-67

	void testUser1() {
		login 'user1', 'password1'

		viewAll()
		editReport11()
		deleteReport11()
		grantEdit12()
		grantEdit13()
		editReport20()
	}

	private void viewAll() {
		for (int i = 1; i <= 67; i++) {
			get "/report/show/$i" 
			assertContentContains "report$i"
		}
		for (int i = 68; i <= 100; i++) {
			get "/report/show/$i" 
			assertContentContains 'Access Denied'
		}
	}

	private void editReport11() {
		get '/report/edit/11'
		assertContentContains 'report11'
		assertContentContains 'Edit Report'

		form {
			name 'report11_new'
			clickButton 'Update'
		}

		get '/report/show/11'
		assertContentContains 'report11_new'
	}

	private void deleteReport11() {
		get '/report/delete/11'
		assertContentContains 'Report 11 deleted'
	}

	private void grantEdit12() {
		get '/report/grant/12'
		assertContentContains 'Grant permission for report12'

		form {
			recipient 'user2'
			permission  BasePermission.READ.mask.toString()
			clickButton 'Grant'
		}

		assertContentContains "Permission $BasePermission.READ.mask granted on Report 12 to user2"

		get '/report/grant/12'
		assertContentContains 'Grant permission for report12'

		form {
			recipient 'user2'
			permission  BasePermission.WRITE.mask.toString()
			clickButton 'Grant'
		}

		assertContentContains "Permission $BasePermission.WRITE.mask granted on Report 12 to user2"
	}

	private void grantEdit13() {
		get '/report/grant/13'
		assertContentContains 'Grant permission for report13'

		form {
			recipient 'user2'
			permission  BasePermission.WRITE.mask.toString()
			clickButton 'Grant'
		}

		assertContentContains 'Access Denied'
	}

	private void editReport20() {
		get '/report/edit/20'
		assertContentContains 'report20'
		assertContentContains 'Edit Report'

		form {
			name 'report20_new'
			clickButton 'Update'
		}

		assertContentContains 'Access Denied'
	}
}
