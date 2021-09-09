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
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.file.IOUtils.readFileToString;

public class BackupAction extends InternalAction {

    public BackupAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        makeBackup(context, false);
    }

    protected void makeBackup(ExecutionContext context, boolean partial) {
        try (ExecutionContext.NewSession newContext = context.newSession()) {

            Integer threadCount = (Integer) findProperty("threadCount[]").read(newContext);
            if(threadCount == null || threadCount < 1) {
                threadCount = 1;
            }

            String backupFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

            List<String> excludeTables = partial ? getExcludeTables(context) : new ArrayList<>();

            String backupFilePath = context.getDbManager().getBackupFilePath(backupFileName);
            if (backupFilePath != null) {
                String backupFileLogPath = backupFilePath + ".log";
                String backupFileExtension = backupFilePath.substring(backupFilePath.lastIndexOf("."));

                DataObject backupObject = newContext.addObject((ConcreteCustomClass) findClass("Backup"));
                LocalDateTime currentDateTime = LocalDateTime.now();
                findProperty("date[Backup]").change(currentDateTime.toLocalDate(), newContext, backupObject);
                findProperty("time[Backup]").change(currentDateTime.toLocalTime(), newContext, backupObject);
                findProperty("file[Backup]").change(backupFilePath, newContext, backupObject);
                findProperty("name[Backup]").change(backupFileName + backupFileExtension, newContext, backupObject);
                findProperty("fileLog[Backup]").change(backupFileLogPath, newContext, backupObject);
                findProperty("isMultithread[Backup]").change(threadCount > 1, newContext, backupObject);

                if(partial) {
                    findProperty("partial[Backup]").change(true, newContext, backupObject);
                    for (String excludeTable : excludeTables) {
                        ObjectValue tableObject = findProperty("table[ISTRING[100]]").readClasses(newContext, new DataObject(excludeTable));
                        if (tableObject instanceof DataObject)
                            findProperty("exclude[Backup,Table]").change(true, newContext, backupObject, (DataObject) tableObject);
                    }
                }

                newContext.apply();

                backupObject = new DataObject((Long)backupObject.object, (ConcreteCustomClass)findClass("Backup")); // обновляем класс после backup

                context.getDbManager().backupDB(context, backupFileName, threadCount, excludeTables);

                findProperty("log[Backup]").change(readFileToString(backupFileLogPath), newContext, backupObject);
                newContext.apply();
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
    public ImMap<Property, Boolean> aspectChangeExtProps() {
        try {
            return getChangeProps(findProperty("date[Backup]").property, findProperty("time[Backup]").property);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return null;
        }
    }
}
