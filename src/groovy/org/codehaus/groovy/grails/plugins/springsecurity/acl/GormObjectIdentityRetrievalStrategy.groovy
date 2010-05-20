/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.springsecurity.acl

import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.ObjectIdentityGenerator
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy

/**
 * The default implementation uses Class.forName() which doesn't work in Grails.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class GormObjectIdentityRetrievalStrategy implements ObjectIdentityRetrievalStrategy, ObjectIdentityGenerator {

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy#getObjectIdentity(
	 * 	java.lang.Object)
	 */
	ObjectIdentity getObjectIdentity(domainObject) {
		createObjectIdentity domainObject.id, domainObject.getClass().name
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.ObjectIdentityGenerator#createObjectIdentity(
	 * 	java.io.Serializable, java.lang.String)
	 */
	ObjectIdentity createObjectIdentity(Serializable id, String type) {
	   new ObjectIdentityImpl(type, id)
	}
}
