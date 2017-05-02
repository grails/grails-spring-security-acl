/*
	* Copyright (c) 2013, ABZ Reporting GmbH (www.abz-reporting.com) 
	* All rights reserved.
	*
	* This software is the confidential and proprietary information
	* of ABZ Reporting GmbH ("Confidential Information").  You shall not
	* disclose such Confidential Information and shall use it only
	* in accordance with the terms of the license agreement you
	* entered into with ABZ Reporting GmbH.
	*
	* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
	* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
	* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	* DISCLAIMED.  IN NO EVENT SHALL ABZ REPORTING GMBH OR
	* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
	* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
	* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
	* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
	* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
	* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
	* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
	* SUCH DAMAGE.
	* 
	* The original code is ABRA, an XBRL processor.
	* The Initial Developer of the Original Code is Thomas Klement
	* <thomas.klement@abz-reporting.com>.
	* Contributor(s): Harald Schmitt, Predrag Knezevic
	*
 */

package org.codehaus.groovy.grails.plugins.springsecurity.acl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import grails.plugins.springsecurity.Secured;

/**
 * Borrowed from https://github.com/alkemist/grails-aop-reloading-fix/blob/master/src/groovy/grails/plugin/aopreloadingfix/ClassLoaderPerProxyGroovyAwareAspectJAwareAdvisorAutoProxyCreator.groovy
 * 
 * It creates proxies for all beans annotated with secure annotations. By default controllers are excluded because
 * they are handled in spring security core plugin. 
 * @author Predrag Knezevic
 */
public class ClassLoaderPerProxyAnnotatedBeanAutoProxyCreator extends
        AbstractAutoProxyCreator {
    
    private static final long serialVersionUID = 1;
    
    private GrailsApplication grailsApplication;
    
    // securing controllers is done in core plugin, but for the controllers where actions
    // are defined as procedures (possible with Grails 2.x), setting the property value
    // to true would secure the controllers as well.
    private boolean secureControllers = false;
    
    @SuppressWarnings("unchecked")
    private final Class<? extends Annotation>[] ANNOTATIONS = new Class[] {Secured.class, 
                                                        org.springframework.security.access.annotation.Secured.class, 
                                                        PreAuthorize.class,
                                                        PreFilter.class, 
                                                        PostAuthorize.class, 
                                                        PostFilter.class};
    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
            TargetSource targetSource) throws BeansException {
        boolean hasSpringSecurityACL = GrailsClassUtils.isStaticProperty(beanClass, "springSecurityACL");
        if (hasSpringSecurityACL || (beanIsAnnotated(beanClass) && (secureControllers || !isControllerClass(beanClass)))) {
            if (logger.isDebugEnabled()) logger.debug("Secure '"+beanName+"' instances of "+beanClass.getName());
            return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
        } else {
            return DO_NOT_PROXY; 
        }       
    }

    private boolean beanIsAnnotated(Class<?> clazz) {
        for (Class<? extends Annotation> annotation: ANNOTATIONS) {
            if (beanIsAnnotated(clazz, annotation)) {
                return true;
            }
        }
        return false;
    }

    private boolean beanIsAnnotated(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz.isAnnotationPresent(annotation)) {
            return true;
        }

        for (Method method: clazz.getMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                return true;
            }
        }

        return false;
    }

    private boolean isControllerClass(Class<?> clazz) {
        for (GrailsClass cc: grailsApplication.getArtefacts("Controller")) {
            if (cc.getClazz().equals(clazz)) {
                return true;
            }
        }
        return false;
    }
    
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void setSecureControllers(boolean secureControllers) {
        this.secureControllers = secureControllers;
    }
    
}
