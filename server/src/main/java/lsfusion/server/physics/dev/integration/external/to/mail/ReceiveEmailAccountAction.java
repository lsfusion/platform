package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.log4j.Logger;

import java.util.Iterator;

import static lsfusion.base.BaseUtils.trim;
import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ReceiveEmailAccountAction extends InternalAction {
    private final ClassPropertyInterface accountInterface;
    private final static Logger logger = ServerLoggers.mailLogger;
    EmailLogicsModule emailLM;

    public ReceiveEmailAccountAction(EmailLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        this.emailLM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        accountInterface = i.next();

        SendEmailAction.setDrawOptions(this);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        if(context.getDbManager().isServer()) {

            try {

                DataObject accountObject = context.getDataKeyValue(accountInterface);
                if (emailLM.disableAccount.read(context, accountObject) != null) {
                    logError(context, localize("{mail.disabled}"));
                    return;
                }

                String receiveHost = trim((String) emailLM.receiveHostAccount.read(context, accountObject));
                if (receiveHost == null) {
                    logError(context, localize("{mail.pop3.host.not.specified.letters.will.not.be.received}"));
                    return;
                }

                Integer receivePort = (Integer) emailLM.receivePortAccount.read(context, accountObject);
                String user = trimToEmpty((String) emailLM.nameAccount.read(context, accountObject));
                String password = trimToEmpty((String) emailLM.passwordAccount.read(context, accountObject));
                AccountType accountType = AccountType.get((String) emailLM.nameReceiveAccountTypeAccount.read(context, accountObject));
                boolean startTLS = emailLM.startTLS.read(context, accountObject) != null;
                boolean deleteMessages = emailLM.deleteMessagesAccount.read(context, accountObject) != null;
                Integer lastDays = (Integer) emailLM.lastDaysAccount.read(context, accountObject);
                Integer maxMessages = (Integer) emailLM.maxMessagesAccount.read(context, accountObject);

                EmailReceiver.receiveEmail(context, emailLM, accountObject, receiveHost, receivePort, user, password, accountType, startTLS, deleteMessages, lastDays, maxMessages);

            } catch (Exception e) {
                logger.error(localize("{mail.failed.to.receive.mail}"), e);
                context.delayUserInterfaction(new MessageClientAction(localize("{mail.failed.to.receive.mail}") + " : " + e, localize("{mail.receiving}")));
            }
        } else {
            logger.info("Email Server disabled, change serverComputer() to enable");
        }
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.receiving}")));
    }
}
