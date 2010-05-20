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
package org.codehaus.groovy.grails.plugins.springsecurity.acl;

import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.model.AccessControlEntry;

/**
 * No-op logger that gets registered as the logger so there's a bean to override.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class NullAclAuditLogger implements AuditLogger {

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.domain.AuditLogger#logIfNeeded(
	 * 	boolean, org.springframework.security.acls.model.AccessControlEntry)
	 */
	public void logIfNeeded(final boolean granted, final AccessControlEntry ace) {
		// do nothing
	}
}
