import org.springframework.security.acls.domain.BasePermission

class User2FunctionalTests extends AbstractSecurityWebTest {

	// user2 has read on 1-5, write on 5

	void testUser2() {
		login 'user2', 'password2'

		viewAll()
		editReport1()
		deleteReport1()
		grantEdit2()
		editReport5()
		editReport12()
		checkListIsFiltered()
	}

	private void viewAll() {
		for (int i = 1; i <= 100; i++) {
			get "/report/show/$i"
			if (i < 6 || i == 12) {
				// view for 12 is granted by user1
				assertContentContains "report$i"
			}
			else {
				assertContentContains 'Access Denied'
			}
		}
	}

	private void editReport1() {
		get '/report/edit/1'
		assertContentContains 'report1'
		assertContentContains 'Edit Report'

		form {
			name 'report1_new'
			clickButton 'Update'
		}

		assertContentContains 'Access Denied'
	}

	private void deleteReport1() {
		get '/report/delete/1'
		assertContentContains 'Access Denied'
	}

	private void grantEdit2() {
		get '/report/grant/2'
		assertContentContains 'Grant permission for report2'

		form {
			recipient 'user2'
			permission  BasePermission.WRITE.mask.toString()
			clickButton 'Grant'
		}

		assertContentContains 'Access Denied'
	}

	private void editReport5() {
		get '/report/edit/5'
		assertContentContains 'report5'
		assertContentContains 'Edit Report'

		form {
			name 'report5_new'
			clickButton 'Update'
		}

		get '/report/show/5'
		assertContentContains 'report5_new'
	}

	private void editReport12() {

		// gets write permission granted by user1 tests

		get '/report/edit/12'
		assertContentContains 'report12'
		assertContentContains 'Edit Report'

		form {
			name 'report12_new'
			clickButton 'Update'
		}

		get '/report/show/12'
		assertContentContains 'report12_new'
	}

	private void checkListIsFiltered() {

		get '/report/list'
		assertContentContains 'report5'
		assertContentDoesNotContain 'report6'

		get '/report/list?offset=80&max=10'
		assertContentContains 'Next'
		assertContentDoesNotContain 'report85'
	}
}
