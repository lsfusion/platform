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
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class ReceiveEmailActionProperty extends ScriptingActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;
    EmailLogicsModule emailLM;


    public ReceiveEmailActionProperty(EmailLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
        this.emailLM = LM;

        askConfirm = true;
        setImage("email.png");
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            ObjectValue accountObject = LM.findLCPByCompoundOldName("defaultInboxAccount").readClasses(context);
            if (accountObject instanceof NullValue) {
                logError(context, getString("mail.default.email.not.specified"));
                return;
            }
            if (emailLM.disableAccount.read(context, accountObject) != null) {
                logError(context, getString("mail.disabled"));
                return;
            }

            String pop3HostAccount = (String) emailLM.pop3HostAccount.read(context, accountObject);
            String nameAccount = (String) emailLM.nameAccount.read(context, accountObject);
            String passwordAccount = (String) emailLM.passwordAccount.read(context, accountObject);
            boolean deleteMessagesAccount = emailLM.deleteMessagesAccount.read(context, accountObject) != null;

            receiveEmail(context, (DataObject) accountObject, pop3HostAccount, nameAccount, passwordAccount,
                    deleteMessagesAccount);

        } catch (Exception e) {
            logError(context, getString("mail.failed.to.receive.mail") + " : " + e.toString());
            e.printStackTrace();
        }
    }

    private void receiveEmail(ExecutionContext context, DataObject accountObject, String pop3HostAccount,
                              String nameAccount, String passwordAccount, boolean deleteMessagesAccount) throws MessagingException, IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        if (pop3HostAccount == null) {
            logError(context, getString("mail.pop3.host.not.specified.letters.will.not.be.received"));
            return;
        }

        EmailReceiver receiver = new EmailReceiver(emailLM, accountObject, nullTrim(pop3HostAccount),
                nullTrim(nameAccount), nullTrim(passwordAccount), deleteMessagesAccount);

        receiver.receiveEmail(context);
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, getString("mail.receiving")));
    }
}
