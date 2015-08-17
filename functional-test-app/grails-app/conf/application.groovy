grails {
	plugin {
		springsecurity {

			controllerAnnotations.staticRules = [
				'/':                ['permitAll'],
				'/error':           ['permitAll'],
				'/index':           ['permitAll'],
				'/index.gsp':       ['permitAll'],
				'/shutdown':        ['permitAll'],
				'/assets/**':       ['permitAll'],
				'/**/js/**':        ['permitAll'],
				'/**/css/**':       ['permitAll'],
				'/**/images/**':    ['permitAll'],
				'/**/favicon.ico':  ['permitAll']
			]

			authority.className = 'com.testacl.Role'
			debug.useFilter = true
			fii.rejectPublicInvocations = false
			logout.postOnly = false
			rejectIfNoRule = false
			roleHierarchy = 'ROLE_ADMIN > ROLE_USER'
			userLookup {
				authorityJoinClassName = 'com.testacl.UserRole'
				userDomainClassName = 'com.testacl.User'
			}
		}
	}
}
