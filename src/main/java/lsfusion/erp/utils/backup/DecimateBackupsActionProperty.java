package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
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
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataSession session = context.createSession();

            long currentDate = Calendar.getInstance().getTime().getTime();
            long month = new Long("2592000000"); // 30 * 24 * 3600 * 1000
            long week = new Long("604800000"); // 7 * 24 * 3600 * 1000

            KeyExpr backupExpr = new KeyExpr("Backup");
            ImRevMap<Object, KeyExpr> backupKeys = MapFact.<Object, KeyExpr>singletonRev("Backup", backupExpr);

            QueryBuilder<Object, Object> backupQuery = new QueryBuilder<Object, Object>(backupKeys);
            backupQuery.addProperty("dateBackup", getLCP("dateBackup").getExpr(session.getModifier(), backupExpr));
            backupQuery.addProperty("fileDeletedBackup", getLCP("fileDeletedBackup").getExpr(session.getModifier(), backupExpr));

            backupQuery.and(getLCP("dateBackup").getExpr(session.getModifier(), backupExpr).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> backupResult = backupQuery.executeClasses(session);

            for (int i = 0, result = backupResult.size(); i < result; i++) {
                DataObject backupObject = backupResult.getKey(i).valueIt().iterator().next();

                Date dateBackup = (Date) backupResult.getValue(i).get("dateBackup").getValue();
                boolean deletedBackup = backupResult.getValue(i).get("fileDeletedBackup").getValue() != null;
                long delta = currentDate - dateBackup.getTime();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateBackup);
                //Если старше недели - оставляем только за понедельник, если старше месяца, только за первое число.
                if (!deletedBackup && ((delta > month && calendar.get(Calendar.DAY_OF_MONTH) != 1) || (delta < month && delta > week && calendar.get(Calendar.DAY_OF_WEEK) != 2)))
                    getLAP("deleteBackup").execute(session, backupObject);

            }

            session.apply(context);

            getLAP("formRefresh").execute(context);

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        try {
            return getChangeProps((CalcProperty) getLCP("dateBackup").property, (CalcProperty) getLCP("timeBackup").property);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return null;
        }
    }
}
