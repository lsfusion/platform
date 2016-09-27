package lsfusion.server.mail;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
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
import static lsfusion.server.context.ThreadLocalContext.localize;

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

            KeyExpr accountExpr = new KeyExpr("account");
            ImRevMap<Object, KeyExpr> accountKeys = MapFact.singletonRev((Object) "account", accountExpr);

            QueryBuilder<Object, Object> accountQuery = new QueryBuilder<>(accountKeys);
            accountQuery.addProperty("receiveHostAccount", emailLM.receiveHostAccount.getExpr(accountExpr));
            accountQuery.addProperty("receivePortAccount", emailLM.receivePortAccount.getExpr(accountExpr));
            accountQuery.addProperty("nameAccount", emailLM.nameAccount.getExpr(accountExpr));
            accountQuery.addProperty("passwordAccount", emailLM.passwordAccount.getExpr(accountExpr));
            accountQuery.addProperty("nameReceiveAccountTypeAccount", emailLM.nameReceiveAccountTypeAccount.getExpr(accountExpr));
            accountQuery.addProperty("deleteMessagesAccount", emailLM.deleteMessagesAccount.getExpr(accountExpr));
            accountQuery.addProperty("lastDaysAccount", emailLM.lastDaysAccount.getExpr(accountExpr));
            accountQuery.and(emailLM.enableAccount.getExpr(accountExpr).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> accountResult = accountQuery.executeClasses(context);

            if(accountResult.isEmpty())
                logError(context, localize("{mail.disabled}"));

            for (int i = 0, size = accountResult.size(); i < size; i++) {
                String nameAccount = null;
                try {
                    DataObject accountObject = accountResult.getKey(i).get("account");
                    ImMap<Object, ObjectValue> accountValues = accountResult.getValue(i);
                    String receiveHostAccount = (String) accountValues.get("receiveHostAccount").getValue();
                    Integer receivePortAccount = (Integer) accountValues.get("receivePortAccount").getValue();
                    nameAccount = (String) accountValues.get("nameAccount").getValue();
                    String passwordAccount = (String) accountValues.get("passwordAccount").getValue();
                    String nameReceiveAccountTypeAccount = (String) accountValues.get("nameReceiveAccountTypeAccount").getValue();
                    boolean isPop3Account = nameReceiveAccountTypeAccount == null || nullTrim(nameReceiveAccountTypeAccount).equals("POP3");
                    boolean deleteMessagesAccount = accountValues.get("deleteMessagesAccount").getValue() != null;
                    Integer lastDaysAccount = (Integer) accountValues.get("lastDaysAccount").getValue();

                    receiveEmail(context, accountObject, receiveHostAccount, receivePortAccount, nameAccount, passwordAccount,
                            isPop3Account, deleteMessagesAccount, lastDaysAccount);

                } catch (Exception e) {
                    logError(context, localize("{mail.failed.to.receive.mail}") + " " + nameAccount + " : " + e.toString());
                    e.printStackTrace();
                }
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
