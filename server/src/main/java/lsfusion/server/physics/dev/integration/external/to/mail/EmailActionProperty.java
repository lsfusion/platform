package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.sql.Timestamp;
import java.util.Date;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public abstract class EmailActionProperty extends ScriptingAction {
    EmailLogicsModule emailLM;
    public static Logger logger = ServerLoggers.mailLogger;

    public EmailActionProperty(EmailLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        this.emailLM = LM;
    }

    public Timestamp getSentDate(Message message) throws MessagingException {
        Date sentDate = message.getSentDate();
        return sentDate == null ? null : new Timestamp(sentDate.getTime());
    }

    public void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.receiving}")));
    }
}