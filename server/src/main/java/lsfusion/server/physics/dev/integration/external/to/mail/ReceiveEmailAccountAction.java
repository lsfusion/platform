package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.nullTrim;
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

        drawOptions.setAskConfirm(true);
        drawOptions.setImage("email.png");
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        if(context.getDbManager().isServer()) {

            try {

                DataObject accountObject = context.getDataKeyValue(accountInterface);
                if (emailLM.disableAccount.read(context, accountObject) != null) {
                    logError(context, localize("{mail.disabled}"));
                    return;
                }

                String receiveHostAccount = (String) emailLM.receiveHostAccount.read(context, accountObject);
                Integer receivePortAccount = (Integer) emailLM.receivePortAccount.read(context, accountObject);
                String nameAccount = (String) emailLM.nameAccount.read(context, accountObject);
                String passwordAccount = (String) emailLM.passwordAccount.read(context, accountObject);
                String nameReceiveAccountTypeAccount = (String) emailLM.nameReceiveAccountTypeAccount.read(context, accountObject);
                AccountType accountType = AccountType.get(nameReceiveAccountTypeAccount);
                boolean deleteMessagesAccount = emailLM.deleteMessagesAccount.read(context, accountObject) != null;
                Integer lastDaysAccount = (Integer) emailLM.lastDaysAccount.read(context, accountObject);
                Integer maxMessagesAccount = (Integer) emailLM.maxMessagesAccount.read(context, accountObject);

                receiveEmail(context, accountObject, receiveHostAccount, receivePortAccount, nameAccount, passwordAccount,
                        accountType, deleteMessagesAccount, lastDaysAccount, maxMessagesAccount);

            } catch (Exception e) {
                logger.error(localize("{mail.failed.to.receive.mail}"), e);
                context.delayUserInterfaction(new MessageClientAction(localize("{mail.failed.to.receive.mail}") + " : " + e, localize("{mail.receiving}")));
            }
        } else {
            logger.info("Email Server disabled, change serverComputer() to enable");
        }
    }

    private void receiveEmail(ExecutionContext context, DataObject accountObject, String receiveHostAccount, Integer receivePortAccount,
                              String nameAccount, String passwordAccount, AccountType accountType, boolean deleteMessagesAccount, Integer lastDaysAccount,
                              Integer maxMessagesAccount)
            throws MessagingException, IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException, GeneralSecurityException {
        if (receiveHostAccount == null) {
            logError(context, localize("{mail.pop3.host.not.specified.letters.will.not.be.received}"));
            return;
        }

        EmailReceiver receiver = new EmailReceiver(emailLM, accountObject, nullTrim(receiveHostAccount),
                receivePortAccount, nullTrim(nameAccount), nullTrim(passwordAccount), accountType, deleteMessagesAccount, lastDaysAccount, maxMessagesAccount);

        receiver.receiveEmail(context);
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.receiving}")));
    }
}
