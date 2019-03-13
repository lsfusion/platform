package lsfusion.server.physics.admin.monitor;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.logics.classes.DateTimeClass;
import lsfusion.server.logics.classes.LongClass;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SQLThreadInfo;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.SessionDataProperty;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static lsfusion.base.BaseUtils.trimToEmpty;

public class UpdateProcessMonitorActionProperty extends ProcessDumpActionProperty {

    public UpdateProcessMonitorActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

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
                active || activeSQL ? sqlJavaActiveThreads : SetFact.<Thread>EMPTY(), active || activeJava, readAllocatedBytes, logSqlProcesses);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        if (active) { // оставляем только javaProcesses + freeProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));
        }
        if (activeJava) { // оставляем javaProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys());
        }

        ImOrderSet<LCP> propsJava = getProps(findProperties("idThreadProcess[VARSTRING[10]]", "stackTraceJavaProcess[VARSTRING[10]]", "nameJavaProcess[VARSTRING[10]]",
                "statusJavaProcess[VARSTRING[10]]", "lockNameJavaProcess[VARSTRING[10]]", "lockOwnerIdProcess[VARSTRING[10]]", "lockOwnerNameProcess[VARSTRING[10]]",
                "nameComputerJavaProcess[VARSTRING[10]]", "nameUserJavaProcess[VARSTRING[10]]", "lsfStackTraceProcess[VARSTRING[10]]",
                "threadAllocatedBytesProcess[VARSTRING[10]]", "lastThreadAllocatedBytesProcess[VARSTRING[10]]"));

        ImOrderSet<LCP> propsSQL = getProps(findProperties("idThreadProcess[VARSTRING[10]]", "dateTimeCallProcess[VARSTRING[10]]",
                "querySQLProcess[VARSTRING[10]]", "addressUserSQLProcess[VARSTRING[10]]",
                "dateTimeSQLProcess[VARSTRING[10]]", "isActiveSQLProcess[VARSTRING[10]]", "inTransactionSQLProcess[VARSTRING[10]]",
                "startTransactionSQLProcess[VARSTRING[10]]", "attemptCountSQLProcess[VARSTRING[10]]", "statusSQLProcess[VARSTRING[10]]",
                "statusMessageSQLProcess[VARSTRING[10]]", "computerProcess[VARSTRING[10]]", "userProcess[VARSTRING[10]]", "lockOwnerIdProcess[VARSTRING[10]]",
                "lockOwnerNameProcess[VARSTRING[10]]", "fullQuerySQLProcess[VARSTRING[10]]", "idSQLProcess[VARSTRING[10]]",
                "isDisabledNestLoopProcess[VARSTRING[10]]", "queryTimeoutProcess[VARSTRING[10]]", "debugInfoSQLProcess[VARSTRING[10]]",
                "threadNameSQLProcess[VARSTRING[10]]", "threadStackTraceSQLProcess[VARSTRING[10]]"));

        int rowsJava = writeRowsJava(context, propsJava, javaProcesses);
        int rowsSQL = writeRowsSQL(context, propsSQL, sqlProcesses);
        if (rowsJava == 0 && rowsSQL == 0)
            findAction("formRefresh[]").execute(context);

    }

    private GetValue<ObjectValue, LCP> getJavaMapValueGetter(final JavaProcess javaProcessValue, final String idThread) {
        return new GetValue<ObjectValue, LCP>() {
            public ObjectValue getMapValue(LCP prop) {
                return getJavaMapValue(prop, javaProcessValue, idThread);
            }
        };
    }

    private GetValue<ObjectValue, LCP> getSQLMapValueGetter(final SQLProcess sqlProcessValue, final String idThread) {
        return new GetValue<ObjectValue, LCP>() {
            public ObjectValue getMapValue(LCP prop) {
                return getSQLMapValue(prop, sqlProcessValue, idThread);
            }
        };
    }

    private ObjectValue getJavaMapValue(LCP<?> prop, JavaProcess javaProcess, String idThread) {
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

    private ObjectValue getSQLMapValue(LCP<?> prop, SQLProcess sqlProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "dateTimeCallProcess":
                return sqlProcess.dateTimeCall == null ? NullValue.instance : new DataObject(sqlProcess.dateTimeCall, DateTimeClass.instance);
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
                return sqlProcess.dateTime == null ? NullValue.instance : new DataObject(sqlProcess.dateTime, DateTimeClass.instance);
            case "isActiveSQLProcess":
                return DataObject.create(sqlProcess.isActive != null && sqlProcess.isActive);
            case "inTransactionSQLProcess":
                if (sqlProcess.baseInTransaction != null && sqlProcess.fusionInTransaction != null && sqlProcess.fusionInTransaction)
                    ServerLoggers.assertLog(sqlProcess.baseInTransaction.equals(true), "FUSION AND BASE INTRANSACTION DIFFERS");
                return DataObject.create(sqlProcess.baseInTransaction != null ? sqlProcess.baseInTransaction : sqlProcess.fusionInTransaction);
            case "startTransactionSQLProcess":
                return sqlProcess.startTransaction == null ? NullValue.instance : new DataObject(new Timestamp(sqlProcess.startTransaction), DateTimeClass.instance);
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

    private int writeRowsJava(ExecutionContext context, final ImOrderSet<LCP> props, ImMap<String, JavaProcess> processes) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> rows;

        rows = processes.mapKeyValues(new GetValue<ImMap<String, DataObject>, String>() {
            public ImMap<String, DataObject> getMapValue(String value) {
                return MapFact.singleton("key", new DataObject(value, StringClass.get(10)));
            }
        }, new GetKeyValue<ImMap<LCP, ObjectValue>, String, JavaProcess>() {
            public ImMap<LCP, ObjectValue> getMapValue(String key, JavaProcess value) {
                return props.getSet().mapValues(getJavaMapValueGetter(value, key));
            }
        });

        SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>("updpm:wr", StringClass.get(10), props, new Type.Getter<LCP>() {
            @Override
            public Type getType(LCP key) {
                return ((LCP<?>)key).property.getType();
            }
        });
        OperationOwner owner = context.getSession().getOwner();
        SQLSession sql = context.getSession().sql;
        importTable.writeRows(sql, rows, owner);

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LCP> importJoin = importTable.join(mapKeys);
        Where where = importJoin.getWhere();
        try {
            for (LCP lcp : props) {
                PropertyChange propChange = new PropertyChange(MapFact.singletonRev(lcp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lcp), where);
                context.getEnv().change((CalcProperty) lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }
        return rows.size();
    }

    private int writeRowsSQL(ExecutionContext context, final ImOrderSet<LCP> props, ImMap<String, SQLProcess> processes) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> rows;

        rows = processes.mapKeyValues(new GetValue<ImMap<String, DataObject>, String>() {
            public ImMap<String, DataObject> getMapValue(String value) {
                return MapFact.singleton("key", new DataObject(value, StringClass.get(10)));
            }
        }, new GetKeyValue<ImMap<LCP, ObjectValue>, String, SQLProcess>() {
            public ImMap<LCP, ObjectValue> getMapValue(String key, SQLProcess value) {
                return props.getSet().mapValues(getSQLMapValueGetter(value, key));
            }
        });

        SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>("updpm:wr", StringClass.get(10), props, new Type.Getter<LCP>() {
            @Override
            public Type getType(LCP key) {
                return ((LCP<?>)key).property.getType();
            }
        });
        OperationOwner owner = context.getSession().getOwner();
        SQLSession sql = context.getSession().sql;
        importTable.writeRows(sql, rows, owner);

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LCP> importJoin = importTable.join(mapKeys);
        Where where = importJoin.getWhere();
        try {
            for (LCP lcp : props) {
                PropertyChange propChange = new PropertyChange(MapFact.singletonRev(lcp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lcp), where);
                context.getEnv().change((CalcProperty) lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }
        return rows.size();
    }

    private ImOrderSet<LCP> getProps(LCP<?>[] properties) {
        return SetFact.fromJavaOrderSet(new ArrayList<LCP>(Arrays.asList(properties))).addOrderExcl(LM.baseLM.importedString);
    }
}