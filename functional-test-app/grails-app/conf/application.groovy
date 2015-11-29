grails {
	plugin {
		springsecurity {

			controllerAnnotations.staticRules = [
				[pattern: '/',                 access: 'permitAll'],
				[pattern: '/error',            access: 'permitAll'],
				[pattern: '/index',            access: 'permitAll'],
				[pattern: '/index.gsp',        access: 'permitAll'],
				[pattern: '/shutdown',         access: 'permitAll'],
				[pattern: '/assets/**',        access: 'permitAll'],
				[pattern: '/**/js/**',         access: 'permitAll'],
				[pattern: '/**/css/**',        access: 'permitAll'],
				[pattern: '/**/images/**',     access: 'permitAll'],
				[pattern: '/**/favicon.ico',   access: 'permitAll']
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
