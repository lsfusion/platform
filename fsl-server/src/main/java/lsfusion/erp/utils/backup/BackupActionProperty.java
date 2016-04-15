package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ConcreteCustomClass;
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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static lsfusion.base.IOUtils.readFileToString;

public class BackupActionProperty extends ScriptingActionProperty {

    public BackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        makeBackup(context, false);
    }

    protected void makeBackup(ExecutionContext context, boolean partial) {
        try (DataSession session = context.createSession()) {

            Date currentDate = Calendar.getInstance().getTime();
            long currentTime = currentDate.getTime();
            String backupFileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(currentDate);

            List<String> excludeTables = partial ? getExcludeTables(context) : new ArrayList<String>();

            String backupFilePath = context.getDbManager().backupDB(context, backupFileName, excludeTables);
            if (backupFilePath != null) {
                String backupFileLogPath = backupFilePath + ".log";
                String backupFileExtension = backupFilePath.substring(backupFilePath.lastIndexOf("."), backupFilePath.length());

                DataObject backupObject = session.addObject((ConcreteCustomClass) findClass("Backup"));
                findProperty("date[Backup]").change(new java.sql.Date(currentTime), session, backupObject);
                findProperty("time[Backup]").change(new java.sql.Time(currentTime), session, backupObject);
                findProperty("file[Backup]").change(backupFilePath, session, backupObject);
                findProperty("name[Backup]").change(backupFileName + backupFileExtension, session, backupObject);
                findProperty("fileLog[Backup]").change(backupFileLogPath, session, backupObject);
                findProperty("log[Backup]").change(readFileToString(backupFileLogPath), session, backupObject);

                if(partial) {
                    findProperty("partial[Backup]").change(true, session, backupObject);
                    for (String excludeTable : excludeTables) {
                        ObjectValue tableObject = findProperty("table[VARISTRING[100]]").readClasses(session, new DataObject(excludeTable));
                        if (tableObject instanceof DataObject)
                            findProperty("exclude[Backup,Table]").change(true, session, backupObject, (DataObject) tableObject);
                    }
                }

                session.apply(context);

                findProperty("backupFilePath[]").change(backupFilePath, context.getSession());
                findProperty("backupFileName[]").change(backupFileName + backupFileExtension, context.getSession());

                findAction("formRefresh[]").execute(context);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private List<String> getExcludeTables(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr tableExpr = new KeyExpr("Table");
        ImRevMap<Object, KeyExpr> tableKeys = MapFact.<Object, KeyExpr>singletonRev("Table", tableExpr);

        QueryBuilder<Object, Object> tableQuery = new QueryBuilder<>(tableKeys);
        tableQuery.addProperty("sidTable", findProperty("sid[Table]").getExpr(context.getModifier(), tableExpr));
        tableQuery.and(findProperty("exclude[Table]").getExpr(context.getModifier(), tableExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> tableResult = tableQuery.execute(context.getSession());

        List<String> excludeTables = new ArrayList<>();
        for (ImMap<Object, Object> entry : tableResult.values()) {

            String sidTable = (String) entry.get("sidTable");
            if (sidTable != null)
                excludeTables.add(sidTable.trim());
        }
        return excludeTables;
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
