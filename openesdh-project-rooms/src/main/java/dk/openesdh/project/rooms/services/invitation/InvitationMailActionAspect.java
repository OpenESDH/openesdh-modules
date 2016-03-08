package dk.openesdh.project.rooms.services.invitation;

import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.service.cmr.action.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.stereotype.Component;

import dk.openesdh.repo.services.actions.OpeneActionServiceAspect;

/**
 * This is a fix for the alfresco invitation mail action to set default locale for email messages.
 * The InviteSender doesn't provide recipient locale and sets recipient email, not user name, as action parameter.
 * The alfresco mail action can obtain locale from preferences only if recipient's user name is provided. 
 * @author rudinjur
 *
 */
@Component
public class InvitationMailActionAspect {
    
    private static final String MSG_EMAIL_SUBJECT = "invitation.invitesender.email.subject";

    @Autowired
    @Qualifier("OpeneActionServiceAspect")
    private OpeneActionServiceAspect openeActionServiceAspect;

    @PostConstruct
    public void init() {
        Predicate<Action> predicate = action -> MSG_EMAIL_SUBJECT
                .equals(action.getParameterValue(MailActionExecuter.PARAM_SUBJECT));
        openeActionServiceAspect.addBeforeActionInterceptor(predicate, this::beforeExecuteAction);
    }

    private void beforeExecuteAction(Action mail) {
        mail.setParameterValue(MailActionExecuter.PARAM_LOCALE, I18NUtil.getLocale());
    }
}
