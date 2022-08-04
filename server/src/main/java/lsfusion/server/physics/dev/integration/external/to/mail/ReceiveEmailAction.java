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
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.trim;
import static lsfusion.base.BaseUtils.trimToEmpty;
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
                String user = null;
                try {
                    DataObject accountObject = accountResult.getKey(i).get("account");
                    ImMap<Object, ObjectValue> accountValues = accountResult.getValue(i);
                    String receiveHost = trim((String) accountValues.get("receiveHostAccount").getValue());
                    if (receiveHost == null) {
                        logError(context, localize("{mail.pop3.host.not.specified.letters.will.not.be.received}"));
                        return;
                    }
                    Integer receivePort = (Integer) accountValues.get("receivePortAccount").getValue();
                    user = trimToEmpty((String) accountValues.get("nameAccount").getValue());
                    String password = trimToEmpty((String) accountValues.get("passwordAccount").getValue());
                    AccountType accountType = AccountType.get((String) accountValues.get("nameReceiveAccountTypeAccount").getValue());
                    boolean startTLS = accountValues.get("startTLS").getValue() != null;
                    boolean deleteMessages = accountValues.get("deleteMessagesAccount").getValue() != null;
                    Integer lastDays = (Integer) accountValues.get("lastDaysAccount").getValue();
                    Integer maxMessages = (Integer) accountValues.get("maxMessagesAccount").getValue();

                    EmailReceiver.receiveEmail(context, emailLM, accountObject, receiveHost, receivePort, user, password, accountType, startTLS, deleteMessages, lastDays, maxMessages);

                } catch (Exception e) {
                    String message = localize("{mail.failed.to.receive.mail}") + ", account: " + user;
                    logger.error(message, e);
                    throw new RuntimeException(message, e);
                }
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
