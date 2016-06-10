package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;

public class DecimateBackupsActionProperty extends ScriptingActionProperty {

    public DecimateBackupsActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try (DataSession session = context.createSession()) {

            boolean saveFirstDay = findProperty("saveFirstDayBackups[]").read(context) != null;
            boolean saveMonday = findProperty("saveMondayBackups[]").read(context) != null;
            Integer maxQuantity = (Integer) findProperty("maxQuantityBackups[]").read(context);
            if(maxQuantity == null && !saveFirstDay && !saveMonday)
                maxQuantity = 30;

            long currentDate = Calendar.getInstance().getTime().getTime();
            long month = new Long("2592000000"); // 30 * 24 * 3600 * 1000
            long week = new Long("604800000"); // 7 * 24 * 3600 * 1000

            KeyExpr backupExpr = new KeyExpr("Backup");
            ImRevMap<Object, KeyExpr> backupKeys = MapFact.<Object, KeyExpr>singletonRev("Backup", backupExpr);

            QueryBuilder<Object, Object> backupQuery = new QueryBuilder<>(backupKeys);
            backupQuery.addProperty("dateBackup", findProperty("date[Backup]").getExpr(session.getModifier(), backupExpr));
            backupQuery.and(findProperty("fileDeleted[Backup]").getExpr(session.getModifier(), backupExpr).getWhere().not());

            backupQuery.and(findProperty("date[Backup]").getExpr(session.getModifier(), backupExpr).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> backupResult = backupQuery.executeClasses(session);

            int count = 0;
            for (int i = backupResult.size() - 1; i >= 0; i--) {
                DataObject backupObject = backupResult.getKey(i).getObject("Backup");

                Date dateBackup = (Date) backupResult.getValue(i).get("dateBackup").getValue();
                long delta = currentDate - dateBackup.getTime();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateBackup);
                boolean limit = maxQuantity != null && count >= maxQuantity;
                boolean firstDay = calendar.get(Calendar.DAY_OF_MONTH) == 1 && saveFirstDay;
                boolean monday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && saveMonday;
                //Если превышен лимит кол-ва, удаляем;
                //Если старше недели, оставляем только за понедельник и за первое число;
                //Если старше месяца, только за первое число.
                if (limit || (delta > month && !firstDay) || (delta < month && delta > week && !firstDay && !monday))
                    findAction("delete[Backup]").execute(session, backupObject);
                else
                    count++;
            }

            session.apply(context);

            findAction("formRefresh[]").execute(context);

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        try {
            return getChangeProps((CalcProperty) findProperty("date[Backup]").property, (CalcProperty) findProperty("time[Backup]").property);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return null;
        }
    }
}
