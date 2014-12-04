package lsfusion.server.mail;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class ReceiveEmailActionProperty extends ScriptingActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;
    EmailLogicsModule emailLM;


    public ReceiveEmailActionProperty(EmailLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
        this.emailLM = LM;

        askConfirm = true;
        setImage("email.png");
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        if(context.getDbManager().isServer()) {

            try {

                ObjectValue accountObject = findProperty("defaultInboxAccount").readClasses(context);
                if (accountObject instanceof NullValue) {
                    logError(context, getString("mail.default.email.not.specified"));
                    return;
                }
                if (emailLM.disableAccount.read(context, accountObject) != null) {
                    logError(context, getString("mail.disabled"));
                    return;
                }

                String receiveHostAccount = (String) emailLM.receiveHostAccount.read(context, accountObject);
                String nameAccount = (String) emailLM.nameAccount.read(context, accountObject);
                String passwordAccount = (String) emailLM.passwordAccount.read(context, accountObject);
                String nameReceiveAccountTypeAccount = (String) emailLM.nameReceiveAccountTypeAccount.read(context, accountObject);
                boolean isPop3Account = nameReceiveAccountTypeAccount == null || nullTrim(nameReceiveAccountTypeAccount).equals("POP3");
                boolean deleteMessagesAccount = emailLM.deleteMessagesAccount.read(context, accountObject) != null;

                receiveEmail(context, (DataObject) accountObject, receiveHostAccount, nameAccount, passwordAccount,
                        isPop3Account, deleteMessagesAccount);

            } catch (Exception e) {
                logError(context, getString("mail.failed.to.receive.mail") + " : " + e.toString());
                e.printStackTrace();
            }
        }
    }

    private void receiveEmail(ExecutionContext context, DataObject accountObject, String receiveHostAccount,
                              String nameAccount, String passwordAccount, boolean isPop3, boolean deleteMessagesAccount)
            throws MessagingException, IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException, GeneralSecurityException {
        if (receiveHostAccount == null) {
            logError(context, getString("mail.pop3.host.not.specified.letters.will.not.be.received"));
            return;
        }

        EmailReceiver receiver = new EmailReceiver(emailLM, accountObject, nullTrim(receiveHostAccount),
                nullTrim(nameAccount), nullTrim(passwordAccount), isPop3, deleteMessagesAccount);

        receiver.receiveEmail(context);
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, getString("mail.receiving")));
    }
}
