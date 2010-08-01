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

import java.util.List;

import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

/**
 * Copied from the <code>BasicLookupStrategy</code> private inner class.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class StubAclParent implements Acl {

	private static final long serialVersionUID = 1;

	private final Long _id;

	/**
	 * Constructor.
	 * @param id  the id
	 */
	public StubAclParent(final Long id) {
		_id = id;
	}

	/**
	 * Get the id.
	 * @return  the id
	 */
	public Long getId() {
		return _id;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#getEntries()
	 */
	public List<AccessControlEntry> getEntries() {
		throw new UnsupportedOperationException("Stub only");
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#getObjectIdentity()
	 */
	public ObjectIdentity getObjectIdentity() {
		return new ObjectIdentityImpl(getClass(), 0);
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#getOwner()
	 */
	public Sid getOwner() {
		throw new UnsupportedOperationException("Stub only");
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#getParentAcl()
	 */
	public Acl getParentAcl() {
		throw new UnsupportedOperationException("Stub only");
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#isEntriesInheriting()
	 */
	public boolean isEntriesInheriting() {
		throw new UnsupportedOperationException("Stub only");
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#isGranted(
	 * 	java.util.List, java.util.List, boolean)
	 */
	public boolean isGranted(final List<Permission> permission, final List<Sid> sids,
			final boolean administrativeMode) {
		throw new UnsupportedOperationException("Stub only");
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.acls.model.Acl#isSidLoaded(java.util.List)
	 */
	public boolean isSidLoaded(final List<Sid> sids) {
		throw new UnsupportedOperationException("Stub only");
	}
}
