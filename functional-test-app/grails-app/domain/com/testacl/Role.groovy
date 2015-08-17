package com.testacl

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Role implements Serializable {

	private static final long serialVersionUID = 1

	String authority

	Role(String authority) {
		this.authority = authority
	}

	static constraints = {
		authority blank: false, unique: true
	}

	static mapping = {
		cache true
	}
}
