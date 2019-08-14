package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;

public class DecimateBackupsAction extends InternalAction {

    public DecimateBackupsAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {

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
            backupQuery.addProperty("dateBackup", findProperty("date[Backup]").getExpr(newContext.getModifier(), backupExpr));
            backupQuery.addProperty("timeBackup", findProperty("time[Backup]").getExpr(newContext.getModifier(), backupExpr));
            backupQuery.and(findProperty("fileDeleted[Backup]").getExpr(newContext.getModifier(), backupExpr).getWhere().not());
            backupQuery.and(findProperty("date[Backup]").getExpr(newContext.getModifier(), backupExpr).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> backupResult = backupQuery.executeClasses(newContext, MapFact.toOrderMap((Object) "dateBackup", true, "timeBackup", true));

            int count = 0;
            for (int i = 0; i < backupResult.size(); i++) {
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
                if (limit || (delta > month && !firstDay) || (delta < month && delta > week && !firstDay && !monday)) {
                    ServerLoggers.systemLogger.info("Decimate Backups: deleting backup " + dateBackup);
                    findAction("delete[Backup]").execute(newContext, backupObject);
                } else
                    count++;
            }

            newContext.apply();

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ImMap<Property, Boolean> aspectChangeExtProps() {
        try {
            return getChangeProps(findProperty("date[Backup]").property, findProperty("time[Backup]").property);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return null;
        }
    }
}
