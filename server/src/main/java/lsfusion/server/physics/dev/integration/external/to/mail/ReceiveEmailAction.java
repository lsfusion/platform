package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ReceiveEmailAction extends InternalAction {
    private final static Logger logger = ServerLoggers.mailLogger;
    EmailLogicsModule emailLM;


    public ReceiveEmailAction(EmailLogicsModule LM) {
        super(LM);
        this.emailLM = LM;

        drawOptions.setAskConfirm(true);
        drawOptions.setImage("email.png");
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        if(context.getDbManager().isServer()) {

            KeyExpr accountExpr = new KeyExpr("account");
            ImRevMap<Object, KeyExpr> accountKeys = MapFact.singletonRev("account", accountExpr);

            QueryBuilder<Object, Object> accountQuery = new QueryBuilder<>(accountKeys);
            accountQuery.addProperty("receiveHostAccount", emailLM.receiveHostAccount.getExpr(accountExpr));
            accountQuery.addProperty("receivePortAccount", emailLM.receivePortAccount.getExpr(accountExpr));
            accountQuery.addProperty("nameAccount", emailLM.nameAccount.getExpr(accountExpr));
            accountQuery.addProperty("passwordAccount", emailLM.passwordAccount.getExpr(accountExpr));
            accountQuery.addProperty("nameReceiveAccountTypeAccount", emailLM.nameReceiveAccountTypeAccount.getExpr(accountExpr));
            accountQuery.addProperty("startTLS", emailLM.startTLS.getExpr(accountExpr));
            accountQuery.addProperty("deleteMessagesAccount", emailLM.deleteMessagesAccount.getExpr(accountExpr));
            accountQuery.addProperty("lastDaysAccount", emailLM.lastDaysAccount.getExpr(accountExpr));
            accountQuery.addProperty("maxMessagesAccount", emailLM.maxMessagesAccount.getExpr(accountExpr));
            accountQuery.and(emailLM.receiveHostAccount.getExpr(accountExpr).getWhere());
            accountQuery.and(emailLM.disableAccount.getExpr(accountExpr).getWhere().not());

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
                    AccountType accountType = AccountType.get((String) accountValues.get("nameReceiveAccountTypeAccount").getValue());
                    boolean startTLS = accountValues.get("startTLS").getValue() != null;
                    boolean deleteMessagesAccount = accountValues.get("deleteMessagesAccount").getValue() != null;
                    Integer lastDaysAccount = (Integer) accountValues.get("lastDaysAccount").getValue();
                    Integer maxMessagesAccount = (Integer) accountValues.get("maxMessagesAccount").getValue();

                    receiveEmail(context, accountObject, receiveHostAccount, receivePortAccount, nameAccount, passwordAccount,
                            accountType, startTLS, deleteMessagesAccount, lastDaysAccount, maxMessagesAccount);

                } catch (Exception e) {
                    String message = localize("{mail.failed.to.receive.mail}") + ", account: " + nameAccount;
                    logger.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
        } else {
            logger.info("Email Server disabled, change serverComputer() to enable");
        }
    }

    private void receiveEmail(ExecutionContext context, DataObject accountObject, String receiveHostAccount, Integer receivePortAccount,
                              String nameAccount, String passwordAccount, AccountType accountType, boolean startTLS, boolean deleteMessagesAccount, Integer lastDaysAccount,
                              Integer maxMessagesAccount)
            throws MessagingException, IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException, GeneralSecurityException {
        if (receiveHostAccount == null) {
            logError(context, localize("{mail.pop3.host.not.specified.letters.will.not.be.received}"));
            return;
        }

        EmailReceiver receiver = new EmailReceiver(emailLM, accountObject, nullTrim(receiveHostAccount),
                receivePortAccount, nullTrim(nameAccount), nullTrim(passwordAccount), accountType, startTLS, deleteMessagesAccount, lastDaysAccount, maxMessagesAccount);

        receiver.receiveEmail(context);
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.receiving}")));
    }
}
