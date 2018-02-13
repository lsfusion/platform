package lsfusion.server.mail;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.server.context.ThreadLocalContext.localize;

public class ReceiveEmailAccountActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface accountInterface;
    private final static Logger logger = ServerLoggers.mailLogger;
    EmailLogicsModule emailLM;


    public ReceiveEmailAccountActionProperty(EmailLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
        this.emailLM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        accountInterface = i.next();

        drawOptions.setAskConfirm(true);
        drawOptions.setImage("email.png");
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

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
                boolean isPop3Account = nameReceiveAccountTypeAccount == null || nullTrim(nameReceiveAccountTypeAccount).equals("POP3");
                boolean deleteMessagesAccount = emailLM.deleteMessagesAccount.read(context, accountObject) != null;
                Integer lastDaysAccount = (Integer) emailLM.lastDaysAccount.read(context, accountObject);

                receiveEmail(context, accountObject, receiveHostAccount, receivePortAccount, nameAccount, passwordAccount,
                        isPop3Account, deleteMessagesAccount, lastDaysAccount);

            } catch (Exception e) {
                logError(context, localize("{mail.failed.to.receive.mail}") + " : " + e.toString());
                e.printStackTrace();
            }
        } else {
            logger.info("Email Server disabled, change serverComputer() to enable");
        }
    }

    private void receiveEmail(ExecutionContext context, DataObject accountObject, String receiveHostAccount, Integer receivePortAccount,
                              String nameAccount, String passwordAccount, boolean isPop3, boolean deleteMessagesAccount, Integer lastDaysAccount)
            throws MessagingException, IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException, GeneralSecurityException {
        if (receiveHostAccount == null) {
            logError(context, localize("{mail.pop3.host.not.specified.letters.will.not.be.received}"));
            return;
        }

        EmailReceiver receiver = new EmailReceiver(emailLM, accountObject, nullTrim(receiveHostAccount),
                receivePortAccount, nullTrim(nameAccount), nullTrim(passwordAccount), isPop3, deleteMessagesAccount, lastDaysAccount);

        receiver.receiveEmail(context);
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.receiving}")));
    }
}
