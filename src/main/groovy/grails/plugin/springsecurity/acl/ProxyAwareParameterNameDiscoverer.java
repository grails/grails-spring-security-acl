/* Copyright 2009-2014 SpringSource.
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
package grails.plugin.springsecurity.acl;

import grails.plugin.springsecurity.acl.util.ProxyUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

/**
 * CGLIB proxies confuse parameter name discovery since the classes aren't compiled with
 * debug, so find the corresponding method or constructor in the target and use that.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class ProxyAwareParameterNameDiscoverer extends LocalVariableTableParameterNameDiscoverer {

	@Override
	public String[] getParameterNames(final Method method) {
		return super.getParameterNames(ProxyUtils.unproxy(method));
	}

	@Override
	public String[] getParameterNames(@SuppressWarnings("rawtypes") final Constructor constructor) {
		return super.getParameterNames(ProxyUtils.unproxy(constructor));
	}
}
