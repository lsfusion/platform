package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.JavaProcess;
import lsfusion.server.physics.admin.monitor.sql.SQLProcess;
import lsfusion.server.physics.admin.monitor.sql.SQLThreadInfo;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.sqlTimestampToLocalDateTime;

public class UpdateProcessMonitorAction extends ProcessDumpAction {

    public UpdateProcessMonitorAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            boolean readAllocatedBytes = Settings.get().isReadAllocatedBytes();
            String processType = trimToEmpty((String) findProperty("nameProcessType[]").read(context));
            context.getSession().cancel(context.stack, SetFact.singleton((SessionDataProperty) findProperty("processType[]").property));

            updateProcessMonitor(context, processType, readAllocatedBytes);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void updateProcessMonitor(ExecutionContext context, String processType, boolean readAllocatedBytes) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        SQLSyntaxType syntaxType = context.getDbSyntax().getSyntaxType();

        boolean active = processType.endsWith("activeAll");
        boolean activeSQL = processType.isEmpty() || processType.endsWith("activeSQL");
        boolean activeJava = processType.endsWith("activeJava");

        boolean logSqlProcesses = Settings.get().isLogSqlProcesses();

        Map<Integer, SQLThreadInfo> sessionThreadMap = SQLSession.getSQLThreadMap();

        MSet<Thread> mSqlJavaActiveThreads = SetFact.mSet();
        MExclSet<String> mFreeSQLProcesses = SetFact.mExclSet();
        ImMap<String, SQLProcess> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, activeSQL, logSqlProcesses)
                : getMSSQLProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, activeSQL);
        ImSet<Thread> sqlJavaActiveThreads = mSqlJavaActiveThreads.immutable();
        ImSet<String> freeSQLProcesses = mFreeSQLProcesses.immutable();

        ImMap<String, JavaProcess> javaProcesses = getJavaProcesses(activeSQL ? null : ThreadUtils.getAllThreads(),
                active || activeSQL ? sqlJavaActiveThreads : SetFact.EMPTY(), active || activeJava, readAllocatedBytes, logSqlProcesses);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        if (active) { // оставляем только javaProcesses + freeProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));
        }
        if (activeJava) { // оставляем javaProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys());
        }

        ImOrderSet<LP> propsJava = getProps(findProperties("idThreadProcess[STRING[10]]", "stackTraceJavaProcess[STRING[10]]", "nameJavaProcess[STRING[10]]",
                "statusJavaProcess[STRING[10]]", "lockNameJavaProcess[STRING[10]]", "lockOwnerIdProcess[STRING[10]]", "lockOwnerNameProcess[STRING[10]]",
                "nameComputerJavaProcess[STRING[10]]", "nameUserJavaProcess[STRING[10]]", "lsfStackTraceProcess[STRING[10]]",
                "threadAllocatedBytesProcess[STRING[10]]", "lastThreadAllocatedBytesProcess[STRING[10]]"));

        ImOrderSet<LP> propsSQL = getProps(findProperties("idThreadProcess[STRING[10]]", "dateTimeCallProcess[STRING[10]]",
                "querySQLProcess[STRING[10]]", "addressUserSQLProcess[STRING[10]]",
                "dateTimeSQLProcess[STRING[10]]", "isActiveSQLProcess[STRING[10]]", "inTransactionSQLProcess[STRING[10]]",
                "startTransactionSQLProcess[STRING[10]]", "attemptCountSQLProcess[STRING[10]]", "statusSQLProcess[STRING[10]]",
                "statusMessageSQLProcess[STRING[10]]", "computerProcess[STRING[10]]", "userProcess[STRING[10]]", "lockOwnerIdProcess[STRING[10]]",
                "lockOwnerNameProcess[STRING[10]]", "fullQuerySQLProcess[STRING[10]]", "idSQLProcess[STRING[10]]",
                "isDisabledNestLoopProcess[STRING[10]]", "queryTimeoutProcess[STRING[10]]", "debugInfoSQLProcess[STRING[10]]",
                "threadNameSQLProcess[STRING[10]]", "threadStackTraceSQLProcess[STRING[10]]"));

        int rowsJava = writeRowsJava(context, propsJava, javaProcesses);
        int rowsSQL = writeRowsSQL(context, propsSQL, sqlProcesses);
        if (rowsJava == 0 && rowsSQL == 0)
            findAction("formRefresh[]").execute(context);

    }

    private Function<LP, ObjectValue> getJavaMapValueGetter(final JavaProcess javaProcessValue, final String idThread) {
        return prop -> getJavaMapValue(prop, javaProcessValue, idThread);
    }

    private Function<LP, ObjectValue> getSQLMapValueGetter(final SQLProcess sqlProcessValue, final String idThread) {
        return prop -> getSQLMapValue(prop, sqlProcessValue, idThread);
    }

    private ObjectValue getJavaMapValue(LP<?> prop, JavaProcess javaProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "stackTraceJavaProcess":
                return javaProcess.stackTrace == null ? NullValue.instance : new DataObject(javaProcess.stackTrace);
            case "nameJavaProcess":
                return javaProcess.name == null ? NullValue.instance : new DataObject(javaProcess.name);
            case "statusJavaProcess":
                return javaProcess.status == null ? NullValue.instance : new DataObject(javaProcess.status);
            case "lockNameJavaProcess":
                return javaProcess.lockName == null ? NullValue.instance : new DataObject(javaProcess.lockName);
            case "lockOwnerIdProcess":
                return javaProcess.lockOwnerId == null ? NullValue.instance : new DataObject(javaProcess.lockOwnerId);
            case "lockOwnerNameProcess":
                return javaProcess.lockOwnerName == null ? NullValue.instance : new DataObject(javaProcess.lockOwnerName);
            case "nameComputerJavaProcess":
                return javaProcess.computer == null ? NullValue.instance : new DataObject(javaProcess.computer);
            case "nameUserJavaProcess":
                return javaProcess.user == null ? NullValue.instance : new DataObject(javaProcess.user);
            case "lsfStackTraceProcess":
                return javaProcess.lsfStackTrace == null ? NullValue.instance : new DataObject(javaProcess.lsfStackTrace);
            case "threadAllocatedBytesProcess":
                return javaProcess.threadAllocatedBytes == null ? NullValue.instance : new DataObject(javaProcess.threadAllocatedBytes, LongClass.instance);
            case "lastThreadAllocatedBytesProcess":
                return javaProcess.lastThreadAllocatedBytes == null ? NullValue.instance : new DataObject(javaProcess.lastThreadAllocatedBytes, LongClass.instance);
            default:
                return NullValue.instance;
        }
    }

    private ObjectValue getSQLMapValue(LP<?> prop, SQLProcess sqlProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "dateTimeCallProcess":
                return sqlProcess.dateTimeCall == null ? NullValue.instance : new DataObject(sqlProcess.dateTimeCall, DateTimeClass.dateTime);
            case "querySQLProcess":
                return sqlProcess.query == null ? NullValue.instance : new DataObject(sqlProcess.query);
            case "fullQuerySQLProcess":
                return sqlProcess.fullQuery == null ? NullValue.instance : new DataObject(sqlProcess.fullQuery);
            case "userProcess":
                return sqlProcess.user == null ? NullValue.instance : new DataObject(sqlProcess.user);
            case "computerProcess":
                return sqlProcess.computer == null ? NullValue.instance : new DataObject(sqlProcess.computer);
            case "addressUserSQLProcess":
                return sqlProcess.addressUser == null ? NullValue.instance : new DataObject(sqlProcess.addressUser);
            case "dateTimeSQLProcess":
                return sqlProcess.dateTime == null ? NullValue.instance : new DataObject(sqlProcess.dateTime, DateTimeClass.dateTime);
            case "isActiveSQLProcess":
                return DataObject.create(sqlProcess.isActive != null && sqlProcess.isActive);
            case "inTransactionSQLProcess":
                if (sqlProcess.baseInTransaction != null && sqlProcess.fusionInTransaction != null && sqlProcess.fusionInTransaction)
                    ServerLoggers.assertLog(sqlProcess.baseInTransaction.equals(true), "FUSION AND BASE INTRANSACTION DIFFERS");
                return DataObject.create(sqlProcess.baseInTransaction != null ? sqlProcess.baseInTransaction : sqlProcess.fusionInTransaction);
            case "startTransactionSQLProcess":
                return sqlProcess.startTransaction == null ? NullValue.instance : new DataObject(sqlTimestampToLocalDateTime(new Timestamp(sqlProcess.startTransaction)), DateTimeClass.dateTime);
            case "attemptCountSQLProcess":
                return sqlProcess.attemptCount == null ? NullValue.instance : new DataObject(sqlProcess.attemptCount);
            case "statusSQLProcess":
                return sqlProcess.status == null ? NullValue.instance : new DataObject(sqlProcess.status);
            case "statusMessageSQLProcess":
                return sqlProcess.statusMessage == null ? NullValue.instance : new DataObject(sqlProcess.statusMessage.getMessage());
            case "lockOwnerIdProcess":
                return sqlProcess.lockOwnerId == null ? NullValue.instance : new DataObject(sqlProcess.lockOwnerId);
            case "lockOwnerNameProcess":
                return sqlProcess.lockOwnerName == null ? NullValue.instance : new DataObject(sqlProcess.lockOwnerName);
            case "idSQLProcess":
                return sqlProcess.sqlId == null ? NullValue.instance : new DataObject(sqlProcess.sqlId);
            case "isDisabledNestLoopProcess":
                return DataObject.create(sqlProcess.isDisabledNestLoop != null && sqlProcess.isDisabledNestLoop);
            case "queryTimeoutProcess":
                return sqlProcess.queryTimeout == null ? NullValue.instance : new DataObject(sqlProcess.queryTimeout);
            case "debugInfoSQLProcess":
                return sqlProcess.debugInfo == null ? NullValue.instance : new DataObject(sqlProcess.debugInfo);
            case "threadNameSQLProcess":
                return sqlProcess.threadName == null ? NullValue.instance : new DataObject(sqlProcess.threadName);
            case "threadStackTraceSQLProcess":
                return sqlProcess.threadStackTrace == null ? NullValue.instance : new DataObject(sqlProcess.threadStackTrace);
            default:
                return NullValue.instance;
        }
    }

    private int writeRowsJava(ExecutionContext context, final ImOrderSet<LP> props, ImMap<String, JavaProcess> processes) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LP, ObjectValue>> rows;

        rows = processes.mapKeyValues(value -> MapFact.singleton("key", new DataObject(value, StringClass.get(10))), (key, value) -> props.getSet().mapValues(getJavaMapValueGetter(value, key)));

        SingleKeyTableUsage<LP> importTable = new SingleKeyTableUsage<>("updpm:wr", StringClass.get(10), props, key -> ((LP<?>)key).property.getType());
        OperationOwner owner = context.getSession().getOwner();
        SQLSession sql = context.getSession().sql;
        importTable.writeRows(sql, rows, owner);

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LP> importJoin = importTable.join(mapKeys);
        Where where = importJoin.getWhere();
        try {
            for (LP lcp : props) {
                PropertyChange propChange = new PropertyChange(MapFact.singletonRev(lcp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lcp), where);
                context.getEnv().change(lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }
        return rows.size();
    }

    private int writeRowsSQL(ExecutionContext context, final ImOrderSet<LP> props, ImMap<String, SQLProcess> processes) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LP, ObjectValue>> rows;

        rows = processes.mapKeyValues(value -> MapFact.singleton("key", new DataObject(value, StringClass.get(10))), (key, value) -> props.getSet().mapValues(getSQLMapValueGetter(value, key)));

        SingleKeyTableUsage<LP> importTable = new SingleKeyTableUsage<>("updpm:wr", StringClass.get(10), props, key -> ((LP<?>)key).property.getType());
        OperationOwner owner = context.getSession().getOwner();
        SQLSession sql = context.getSession().sql;
        importTable.writeRows(sql, rows, owner);

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LP> importJoin = importTable.join(mapKeys);
        Where where = importJoin.getWhere();
        try {
            for (LP lcp : props) {
                PropertyChange propChange = new PropertyChange(MapFact.singletonRev(lcp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lcp), where);
                context.getEnv().change(lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }
        return rows.size();
    }

    private ImOrderSet<LP> getProps(LP<?>[] properties) {
        return SetFact.fromJavaOrderSet(new ArrayList<LP>(Arrays.asList(properties))).addOrderExcl(LM.baseLM.importedString);
    }
}