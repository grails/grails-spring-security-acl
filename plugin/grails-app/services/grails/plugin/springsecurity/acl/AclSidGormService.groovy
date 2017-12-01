package grails.plugin.springsecurity.acl

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormValidateable
import org.springframework.context.i18n.LocaleContextHolder

@Slf4j
@CompileStatic
class AclSidGormService {

    @ReadOnly
    AclSid findBySidAndPrincipal(String sidName, boolean principal) {
        findQueryBySidAndPrincipal(sidName, principal).get()
    }

    @Transactional
    AclSid saveBySidNameAndPrincipal(String sidName, boolean principal) {
        AclSid aclSidInstance = new AclSid(sid: sidName, principal: principal)
        if ( !aclSidInstance.save() ) {
            log.error '{}', errorsBeanBeingSaved(aclSidInstance)
        }
        aclSidInstance
    }

    protected DetachedCriteria<AclSid> findQueryBySidAndPrincipal(String sidName, boolean principalParam) {
        AclSid.where { sid == sidName && principal == principalParam }
    }

    @CompileDynamic
    String errorsBeanBeingSaved(GormValidateable bean) {
        StringBuilder message = new StringBuilder("problem creating ${bean.getClass().simpleName}: $bean")
        Locale locale = LocaleContextHolder.getLocale()
        for (fieldErrors in bean.errors) {
            for (error in fieldErrors.allErrors) {
                message << '\n\t' << messageSource.getMessage(error, locale)
            }
        }
        message.toString()
    }
}