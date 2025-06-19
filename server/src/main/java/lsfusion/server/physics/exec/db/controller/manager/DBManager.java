package lsfusion.server.physics.exec.db.controller.manager;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.implementations.abs.AMap;
import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.base.col.lru.LRUWWEVSMap;
import lsfusion.base.file.RawFileData;
import lsfusion.base.lambda.E2Runnable;
import lsfusion.interop.ProgressBar;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.base.controller.stack.*;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.task.PublicTask;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.formula.FormulaUnionExpr;
import lsfusion.server.data.expr.formula.MLinearOperandMap;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.formula.StringConcatenateFormulaImpl;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.query.result.ReadBatchResultHandler;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.MigrationScriptLexer;
import lsfusion.server.language.MigrationScriptParser;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.stack.NewThreadExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.controller.init.SessionCreator;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ByteArrayClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.TSVectorClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.interactive.property.PropertyAsync;
import lsfusion.server.logics.form.stat.LimitOffset;
import lsfusion.server.logics.navigator.controller.env.*;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.data.AbstractDataProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyObjectImplement;
import lsfusion.server.logics.property.implement.PropertyObjectInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.integration.service.*;
import lsfusion.server.physics.exec.db.table.*;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.base.ResourceUtils.getRevision;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.directLI;
import static lsfusion.server.physics.admin.log.ServerLoggers.*;

public class DBManager extends LogicsManager implements InitializingBean {
    public static final Logger logger = Logger.getLogger(DBManager.class);
    public static final Logger serviceLogger = ServerLoggers.serviceLogger;

    private Map<String, String> finalPropertyDrawNameChanges = new HashMap<>();
    private Map<String, String> finalNavigatorElementNameChanges = new HashMap<>();

    private DataAdapter adapter;

    private RestartManager restartManager;

    private MigrationManager migrationManager;
    
    private BusinessLogics businessLogics;

    private boolean ignoreMigration;
    private boolean migrationScriptWasRead = false;

    private Boolean denyDropModules;
    private Boolean denyDropTables;

    private String dbNamingPolicy;
    private Integer dbMaxIdLength;

    private BaseLogicsModule LM;
    private ReflectionLogicsModule reflectionLM;
    private SystemEventsLogicsModule systemEventsLM;

    private long systemUser;
    public long serverComputer;

    private final ThreadLocal<SQLSession> threadLocalSql;

    private final Map<ImList<PropertyObjectInterfaceImplement<String>>, IndexOptions> indexes = new HashMap<>();

    private String defaultUserLanguage;
    private String defaultUserCountry;
    private String defaultUserTimezone;
    private Integer defaultUserTwoDigitYearStart;
    private String defaultUserDateFormat;
    private String defaultUserTimeFormat;

    public DBManager() {
        super(DBMANAGER_ORDER);

        threadLocalSql = new ThreadLocal<>();
    }

    public static boolean START_TIL = false;
    public static boolean DEBUG_TIL = false;
    public static boolean ID_TIL = true; // we don't need true serializable
    public static boolean RECALC_CLASSES_TIL = false;
    public static boolean RECALC_MAT_TIL = false;
    public static boolean CHECK_CLASSES_TIL = true;
    public static boolean CHECK_MAT_TIL = true;
    public static boolean PACK_TIL = false;
    public static boolean UPLOAD_TIL = false;

    public static String checkClasses(final SQLSession sql, boolean runInTransaction, BaseClass baseClass) throws SQLException, SQLHandledException {
        return checkClasses(sql, runInTransaction, DataSession.emptyEnv(OperationOwner.unknown), baseClass);
    }

    public static String checkClasses(final SQLSession sql, boolean runInTransaction, final QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {

        final Result<String> incorrect = new Result<>();
        runClassesExclusiveness(sql, runInTransaction, query -> incorrect.set(query.readSelect(sql, env)), baseClass);

        if (!incorrect.result.isEmpty())
            return "---- Checking Classes Exclusiveness -----" + '\n' + incorrect.result;
        return "";
    }

    public void addIndex(Property property) {
        addIndex(new LP(property));
    }

    public void addIndex(LP lp) {
        addIndex(lp, null, IndexType.DEFAULT);
    }

    public void addIndex(LP lp, String dbName, IndexType indexType) {
        ImOrderSet<String> keyNames = SetFact.toOrderExclSet(lp.listInterfaces.size(), i -> "key"+i);
        addIndex(keyNames, dbName, indexType, directLI(lp));
    }

    public void addIndex(ImOrderSet<String> keyNames, String dbName, IndexType indexType, Object... params) {
        ImList<PropertyObjectInterfaceImplement<String>> index = ActionOrPropertyUtils.readObjectImplements(keyNames, params);
        addIndex(index, dbName, indexType);
    }

    public void initReflectionEvents() {
        assert LM == null && reflectionLM == null;
        LM = businessLogics.LM;
        reflectionLM = businessLogics.reflectionLM;
        systemEventsLM = businessLogics.systemEventsLM;

        try {
            ImplementTable.reflectionStatProps(() -> {
                SQLSession sql = getThreadLocalSql();

                // splitting ensure function into two is necessary
                // to make "DROP FUNCTION"-s execute after ensureDB (to have an active connection)
                // but before ensureSqlFuncs.
                adapter.ensureDBConnection(false);

                if(!isFirstStart(sql) && getOldDBStructure(sql).version < 40) {
                    startLog("Migrating cast.sql functions");
                    sql.executeDDL("DROP FUNCTION IF EXISTS cast_json_to_static_file(jsonb)");
                    sql.executeDDL("DROP FUNCTION IF EXISTS cast_json_text_to_static_file(json)");
                }

                adapter.ensureSqlFuncs();

                if (!isFirstStart(sql)) {
                    updateStats(sql, true);

                    startLog("Setting user logging for properties");
                    setUserLoggableProperties(sql);

                    if(getOldDBStructure(sql).version >= 41) {
                        startLog("Setting user materialized for properties");
                        setUserMaterializedProperties(sql);
                    }

                    startLog("Setting user not null constraints for properties");
                    setNotNullProperties(sql);

                    if (getOldDBStructure(sql).version >= 34) {
                        startLog("Disabling input list");
                        setDisableInputListProperties(sql);
                    }

                    if (getOldDBStructure(sql).version >= 39) {
                        startLog("Setting user select for properties");
                        setSelectProperties(sql);
                    }
                }
                return null;
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            LM = null;
            reflectionLM = null;
            systemEventsLM = null;
        }
    }

    // Checks for an absence of a database or for an empty database  
    private boolean isFirstStart(SQLSession sql) throws IOException, SQLException, SQLHandledException {
        boolean prevSuppressErrorLogging = sql.suppressErrorLogging;
        sql.suppressErrorLogging = true;
        
        try {
            OldDBStructure dbStructure = getOldDBStructure(sql);
            return dbStructure.isEmpty();
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof PSQLException) {
                String sqlState = ((PSQLException) cause).getSQLState();
                // PostgreSQL error with code 3D000 (invalid_catalog_name) is thrown when the database is absent,
                // when there is an empty database then error with code 42P01 (undefined_table) is thrown  
                // https://www.postgresql.org/docs/9.4/errcodes-appendix.html
                if ("3D000".equals(sqlState) || "42P01".equals(sqlState)) {
                    return true;
                }
            }
            throw e;
        } finally {
            sql.suppressErrorLogging = prevSuppressErrorLogging;
        }
    }
    
    private DBNamingPolicy namingPolicy;
    public DBNamingPolicy getNamingPolicy() {
        return namingPolicy;
    }

    private final ChangesController changesController = new ChangesController() {
        public DBManager getDbManager() {
            return DBManager.this;
        }
    };

    public static Integer getPropertyInterfaceStat(Property property) {
        Integer statsProperty = null;
        Stat interfaceStat = property.getInterfaceStat(false);
        if (interfaceStat != null)
            statsProperty = interfaceStat.getCount();
        return statsProperty;
    }

    private void setNotNullProperties(SQLSession sql) throws SQLException, SQLHandledException {
        
        LP isProperty = LM.is(reflectionLM.property);
        ImRevMap<Object, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.and(reflectionLM.isSetNotNullProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(sql, OperationOwner.unknown);

        for (ImMap<Object, Object> values : result.valueIt()) {
            LP<?> prop = businessLogics.findProperty(values.get("CNProperty").toString().trim());
            if(prop != null) {
                prop.property.userNotNull = true;
                LM.setNotNull(prop.property, null, ListFact.EMPTY(), null, Event.APPLY);
            }
        }
    }

    private void setDisableInputListProperties(SQLSession sql) throws SQLException, SQLHandledException {
        ImRevMap<Object, KeyExpr> keys = LM.is(reflectionLM.property).getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.and(reflectionLM.disableInputListProperty.getExpr(key).getWhere());

        for (ImMap<Object, Object> values : query.execute(sql, OperationOwner.unknown).valueIt()) {
            LP<?> prop = businessLogics.findProperty(values.get("CNProperty").toString().trim());
            if(prop != null)
                LM.disableInputList(prop);
        }
    }

    private void setSelectProperties(SQLSession sql) throws SQLException, SQLHandledException {
        ImRevMap<Object, KeyExpr> keys = LM.is(reflectionLM.property).getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.addProperty("select", reflectionLM.nameSelectProperty.getExpr(key));
        query.and(reflectionLM.nameSelectProperty.getExpr(key).getWhere());

        for (ImMap<Object, Object> values : query.execute(sql, OperationOwner.unknown).valueIt()) {
            LP<?> prop = businessLogics.findProperty(values.get("CNProperty").toString().trim());
            String select = (String) values.get("select");
            if(prop != null)
                prop.property.setSelect(select);
        }
    }

    public void checkIndexes(SQLSession session) throws SQLException, SQLHandledException {
        try {
            for (Map.Entry<ImplementTable, List<IndexData<Field>>> mapIndex : getIndexesMap().entrySet()) {
                session.startTransaction(START_TIL, OperationOwner.unknown);
                ImplementTable table = mapIndex.getKey();
                for (IndexData<Field> index : mapIndex.getValue()) {
                    ImOrderSet<Field> fields = SetFact.fromJavaOrderSet(index.fields);
                    session.checkIndex(table, table.keys, fields, index.options);
                }
                session.addConstraint(table);
                session.checkExtraIndexes(getThreadLocalSql(), table, table.keys);
                session.commitTransaction();
            }
        } catch (Exception e) {
            session.rollbackTransaction();
            throw e;
        }
    }

    public void checkTables(SQLSession session) throws SQLException, SQLHandledException {
        try {
            for (ImplementTable table : getIndexesMap().keySet()) {
                session.startTransaction(START_TIL, OperationOwner.unknown);
                session.createTable(table, table.keys, true);
                for (PropertyField property : table.properties)
                    session.addColumn(table, property, true);
                session.commitTransaction();
            }
        } catch (Exception e) {
            session.rollbackTransaction();
            throw e;
        }
    }

    public void firstRecalculateStatsAndMaterializations(DataSession session) throws SQLException, SQLHandledException {
        if(reflectionLM.hasNotNullQuantity.read(session) == null) {
            recalculateStats(session);
            NewThreadExecutionStack stack = ThreadLocalContext.getStack();
            recalculateMaterializations(stack, session.sql, businessLogics.serviceLM.singleTransaction.read(session) == null);
            session.applyException(businessLogics, stack);
        }
    }

    private void updateClassStats(SQLSession session, boolean useSIDs) throws SQLException, SQLHandledException {
        if(useSIDs)
            updateClassSIDStats(session);
        else
            updateClassStats(session);
    }

    private void updateClassStats(SQLSession session) throws SQLException, SQLHandledException {
        ImMap<Long, Integer> customObjectClassMap = readClassStatsFromDB(session);

        for(CustomClass customClass : LM.baseClass.getAllClasses()) {
            if(customClass instanceof ConcreteCustomClass) {
                ((ConcreteCustomClass) customClass).updateStat(customObjectClassMap);
            }
        }
    }

    private ImMap<Long, Integer> readClassStatsFromDB(SQLSession session) throws SQLException, SQLHandledException {
        KeyExpr customObjectClassExpr = new KeyExpr("customObjectClass");
        ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev("key", customObjectClassExpr);

        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("statCustomObjectClass", LM.statCustomObjectClass.getExpr(customObjectClassExpr));

        query.and(LM.statCustomObjectClass.getExpr(customObjectClassExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session, OperationOwner.unknown);

        MExclMap<Long, Integer> mCustomObjectClassMap = MapFact.mExclMap(result.size());
        for (int i=0,size=result.size();i<size;i++) {
            Integer statCustomObjectClass = (Integer) result.getValue(i).get("statCustomObjectClass");
            mCustomObjectClassMap.exclAdd((Long) result.getKey(i).get("key"), statCustomObjectClass);
        }
        return mCustomObjectClassMap.immutable();
    }

    private void updateClassSIDStats(SQLSession session) throws SQLException, SQLHandledException {
        ImMap<String, Integer> customSIDObjectClassMap = readClassSIDStatsFromDB(session);

        for(CustomClass customClass : LM.baseClass.getAllClasses()) {
            if(customClass instanceof ConcreteCustomClass) {
                ((ConcreteCustomClass) customClass).updateSIDStat(customSIDObjectClassMap);
            }
        }
    }

    private ImMap<String, Integer> readClassSIDStatsFromDB(SQLSession session) throws SQLException, SQLHandledException {
        KeyExpr customObjectClassExpr = new KeyExpr("customObjectClass");
        ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev("key", customObjectClassExpr);

        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("statCustomObjectClass", LM.statCustomObjectClass.getExpr(customObjectClassExpr));
        query.addProperty("staticName", LM.staticName.getExpr(customObjectClassExpr));

        query.and(LM.statCustomObjectClass.getExpr(customObjectClassExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session, OperationOwner.unknown);

        MExclMap<String, Integer> mCustomObjectClassMap = MapFact.mExclMapMax(result.size());
        for (int i=0,size=result.size();i<size;i++) {
            Integer statCustomObjectClass = (Integer) result.getValue(i).get("statCustomObjectClass");
            String sID = (String)result.getValue(i).get("staticName");
            if(sID != null)
                mCustomObjectClassMap.exclAdd(sID.trim(), statCustomObjectClass);
        }
        return mCustomObjectClassMap.immutable();
    }

    private void updateStats(SQLSession sql, boolean useSIDsForClasses) throws SQLException, SQLHandledException {
        updateTableStats(sql, true); // чтобы сами таблицы статистики получили статистику
        updateFullClassStats(sql, useSIDsForClasses);
        if(SystemProperties.doNotCalculateStats)
            return;
        updateTableStats(sql, false);
    }

    private void updateFullClassStats(SQLSession sql, boolean useSIDsForClasses) throws SQLException, SQLHandledException {
        updateClassStats(sql, useSIDsForClasses);

        adjustClassStats(sql);        
    }
    
//    businessLogics.* -> *

    private void adjustClassStats(SQLSession sql) throws SQLException, SQLHandledException {
        ImMap<String, Integer> tableStats = readStatsFromDB(sql, reflectionLM.tableSID, reflectionLM.rowsTable, null);
        ImMap<String, Integer> keyStats = readStatsFromDB(sql, reflectionLM.tableKeySID, reflectionLM.overQuantityTableKey, null);

        MMap<CustomClass, Integer> mClassFullStats = MapFact.mMap(MapFact.max());
        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.fillFullClassStat(tableStats, keyStats, mClassFullStats);
        }
        ImMap<CustomClass, Integer> classFullStats = mClassFullStats.immutable();

        // правим статистику по классам
        ImOrderMap<CustomClass, Integer> orderedClassFullStats = classFullStats.sort(BaseUtils.immutableCast(ValueClass.comparator));// для детерминированности
        for(int i=0,size=orderedClassFullStats.size();i<size;i++) {
            CustomClass customClass = orderedClassFullStats.getKey(i);
            int quantity = orderedClassFullStats.getValue(i);
            ImOrderSet<ConcreteCustomClass> concreteChildren = customClass.getUpSet().getSetConcreteChildren().sortSet(BaseUtils.immutableCast(ValueClass.comparator));// для детерминированности
            int childrenStat = 0;
            for(ConcreteCustomClass child : concreteChildren) {
                childrenStat += child.getCount();
            }
            quantity = quantity - childrenStat; // сколько дораспределить
            for(ConcreteCustomClass child : concreteChildren) {
                int count = child.getCount();
                int newCount = (int)((long)quantity * (long)count / (long)childrenStat);
                child.stat = count + newCount;
                assert child.stat >= 0;
                quantity -= newCount;
                childrenStat -= count;
            }
        }
    }

    private void updateTableStats(SQLSession sql, boolean statDefault) throws SQLException, SQLHandledException {
        ImMap<String, Integer> tableStats;
        ImMap<String, Integer> keyStats;
        ImMap<String, Pair<Integer, Integer>> propStats;
        if(statDefault) {
            tableStats = MapFact.EMPTY();
            keyStats = MapFact.EMPTY();
            propStats = MapFact.EMPTY();
        } else {
            tableStats = readStatsFromDB(sql, reflectionLM.tableSID, reflectionLM.rowsTable, null);
            keyStats = readStatsFromDB(sql, reflectionLM.tableKeySID, reflectionLM.overQuantityTableKey, null);
            propStats = readStatsFromDB(sql, reflectionLM.tableColumnLongSID, reflectionLM.overQuantityTableColumn, reflectionLM.notNullQuantityTableColumn);
        }

        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.updateStat(tableStats, keyStats, propStats, statDefault);
        }
    }

    private static <V> ImMap<String, V> readStatsFromDB(SQLSession sql, LP sIDProp, LP statsProp, final LP notNullProp) throws SQLException, SQLHandledException {
        QueryBuilder<String, String> query = new QueryBuilder<>(SetFact.toSet("key"));
        Expr sidToObject = sIDProp.getExpr(query.getMapExprs().singleValue());
        query.and(sidToObject.getWhere());
        query.addProperty("property", statsProp.getExpr(sidToObject));
        if(notNullProp!=null)
            query.addProperty("notNull", notNullProp.getExpr(sidToObject));
        return query.execute(sql, OperationOwner.unknown).getMap().mapKeyValues(key -> ((String) key.singleValue()).trim(), value -> {
            if(notNullProp!=null) {
                return (V) new Pair<>((Integer) value.get("property"), (Integer) value.get("notNull"));
            } else
                return (V)value.singleValue();
        });
    }

    public void recalculateStats(DataSession session) throws SQLException, SQLHandledException {
        int count = 0;
        ImSet<ImplementTable> tables = LM.tableFactory.getImplementTables(getDisableStatsTableSet(session));
        for (ImplementTable dataTable : tables) {
            count++;
            long start = System.currentTimeMillis();
            BaseUtils.serviceLogger.info(String.format("Recalculate Stats %s of %s: %s", count, tables.size(), dataTable));
            dataTable.recalculateStat(reflectionLM, getDisableStatsTableColumnSet(), session);
            long time = System.currentTimeMillis() - start;
            BaseUtils.serviceLogger.info(String.format("Recalculate Stats: %s, %sms", dataTable, time));
        }

        count = 0;
        ImCol<ObjectValueClassSet> tableClassesCol = LM.baseClass.getUpObjectClassFields().values();
        for (ObjectValueClassSet tableClasses : tableClassesCol) {
            count++;
            long start = System.currentTimeMillis();
            BaseUtils.serviceLogger.info(String.format("Recalculate Class Stats %s of %s: %s", count, tableClassesCol.size(), tableClasses));

            QueryBuilder<Integer, Integer> classes = new QueryBuilder<>(SetFact.singleton(0));
            KeyExpr countKeyExpr = new KeyExpr("count");
            Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(LM.baseClass)),
                    ValueExpr.COUNT, countKeyExpr.isClass(tableClasses), GroupType.SUM, classes.getMapExprs());
            classes.addProperty(0, countExpr);
            classes.and(countExpr.getWhere());
            ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);

            ImSet<ConcreteCustomClass> concreteChilds = tableClasses.getSetConcreteChildren();
            MExclMap<Long, Integer> mResult = MapFact.mExclMap(concreteChilds.size());
            for (int j = 0, size = concreteChilds.size(); j < size; j++) {
                ConcreteCustomClass customClass = concreteChilds.get(j);
                ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, customClass.ID));
                int statValue = classStat == null ? 1 : (Integer) classStat.singleValue();
                mResult.exclAdd(customClass.ID, statValue);
                LM.statCustomObjectClass.change(statValue, session, customClass.getClassObject());
            }
            BaseUtils.serviceLogger.info(String.format("Recalculate Class Stats: %s, %sms", tableClasses, System.currentTimeMillis() - start));
        }
    }

    public void overCalculateStats(DataSession session, Integer maxQuantityOverCalculate) throws SQLException, SQLHandledException {
        int count = 0;
        MSet<Long> propertiesSet = businessLogics.getOverCalculatePropertiesSet(session, maxQuantityOverCalculate);
        ImSet<ImplementTable> tables = LM.tableFactory.getImplementTables(getDisableStatsTableSet(session));
        for (ImplementTable dataTable : tables) {
            count++;
            long start = System.currentTimeMillis();
            if(dataTable.overCalculateStat(reflectionLM, session, propertiesSet, getDisableStatsTableColumnSet(),
                    new ProgressBar("Recalculate Stats", count, tables.size(), dataTable.toString()))) {
                long time = System.currentTimeMillis() - start;
                BaseUtils.serviceLogger.info(String.format("Recalculate Stats: %s, %sms", String.valueOf(dataTable), time));
            }
        }
    }

    public static void runClassesExclusiveness(SQLSession session, boolean runInTransaction, DataSession.RunExclusiveness run, BaseClass baseClass) throws SQLException, SQLHandledException {
        run(session, runInTransaction, RECALC_CLASSES_TIL, sql -> runClassesExclusiveness(run, sql, baseClass));
    }

    public static void runClassesExclusiveness(DataSession.RunExclusiveness run, SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {

        // тут можно было бы использовать нижнюю конструкцию, но с учетом того что не все базы поддерживают FULL JOIN, на UNION'ах и их LEFT JOIN'ах с проталкиванием, запросы получаются мегабайтные и СУБД не справляется
//        KeyExpr key = new KeyExpr("key");
//        String incorrect = new Query<String,String>(MapFact.singletonRev("key", key), key.classExpr(baseClass, IsClassType.SUMCONSISTENT).compare(ValueExpr.COUNT, Compare.GREATER)).readSelect(sql, env);

        // пока не вытягивает определение, для каких конкретно классов образовалось пересечение, ни сервер приложение ни СУБД
        final KeyExpr key = new KeyExpr("key");
        final int threshold = 30;
        final ImOrderSet<ObjectClassField> tables = baseClass.getUpObjectClassFields().keys().toOrderSet();

        final MLinearOperandMap mSum = new MLinearOperandMap();
        final MList<Expr> mAgg = ListFact.mList();
        final MAddCol<SingleKeyTableUsage<String>> usedTables = ListFact.mAddCol();
        for(ImSet<ObjectClassField> group : tables.getSet().group(keyField -> tables.indexOf(keyField) % threshold).values()) {
            SingleKeyTableUsage<String> table = new SingleKeyTableUsage<>("runexls", ObjectType.instance, SetFact.toOrderExclSet("sum", "agg"), key1 -> key1.equals("sum") ? ValueExpr.COUNTCLASS : StringClass.getv(false, ExtInt.UNLIMITED));
            Expr sumExpr = IsClassExpr.create(key, group, IsClassType.SUMCONSISTENT);
            Expr aggExpr = IsClassExpr.create(key, group, IsClassType.AGGCONSISTENT);
            table.writeRows(sql, new Query<>(MapFact.singletonRev("key", key), MapFact.toMap("sum", sumExpr, "agg", aggExpr), sumExpr.getWhere()), baseClass, DataSession.emptyEnv(OperationOwner.unknown), SessionTable.nonead);

            Join<String> tableJoin = table.join(key);
            mSum.add(tableJoin.getExpr("sum"), 1);
            mAgg.add(tableJoin.getExpr("agg"));

            usedTables.add(table);
        }

        // FormulaUnionExpr.create(new StringAggConcatenateFormulaImpl(","), mAgg.immutableList()) , "value",
        Expr sumExpr = mSum.getExpr();
        Expr aggExpr = FormulaUnionExpr.create(new StringConcatenateFormulaImpl(","), mAgg.immutableList());
        run.run(new Query<>(MapFact.singletonRev("key", key), sumExpr.compare(ValueExpr.COUNT, Compare.GREATER), MapFact.EMPTY(),
                MapFact.toMap("sum", sumExpr, "agg", aggExpr)));

        for(SingleKeyTableUsage<String> usedTable : usedTables.it())
            usedTable.drop(sql, OperationOwner.unknown);
    }

    public String getDataBaseName() {
        return adapter.dataBase;
    }

    public DataAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DataAdapter adapter) {
        this.adapter = adapter;
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public BusinessLogics getBusinessLogics(){
        return businessLogics;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setIgnoreMigration(boolean ignoreMigration) {
        this.ignoreMigration = ignoreMigration;
    }

    public void setDenyDropModules(Boolean denyDropModules) {
        this.denyDropModules = denyDropModules;
    }

    public boolean isDenyDropModules() {
        return nvl(denyDropModules, !SystemProperties.lightStart);
    }

    public void setDenyDropTables(Boolean denyDropTables) {
        this.denyDropTables = denyDropTables;
    }

    public boolean isDenyDropTables() {
        return nvl(denyDropTables, !SystemProperties.lightStart);
    }

    public String getDbNamingPolicy() {
        return dbNamingPolicy;
    }

    public void setDbNamingPolicy(String dbNamingPolicy) {
        this.dbNamingPolicy = dbNamingPolicy;
    }

    public Integer getDbMaxIdLength() {
        return dbMaxIdLength;
    }

    public void setDbMaxIdLength(Integer dbMaxIdLength) {
        this.dbMaxIdLength = dbMaxIdLength;
    }

    public SQLSyntax getSyntax() {
        return adapter.syntax;
    }
    @Override
    public void afterPropertiesSet() {
        Assert.notNull(adapter, "adapter must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");

        // it is also sort of DI so will do it here
        int maxIdLength = getDbMaxIdLength();
        String policyName = getDbNamingPolicy();
        try {
            namingPolicy = (DBNamingPolicy) ((Class) Class.forName(policyName)).getConstructors()[0].newInstance(maxIdLength);
        } catch (InvocationTargetException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw Throwables.propagate(e);
        }
    }


    private PublicTask initTask;

    public void setInitTask(PublicTask initTask) {
        this.initTask = initTask;
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        this.LM = businessLogics.LM;
        this.reflectionLM = businessLogics.reflectionLM;
        this.systemEventsLM = businessLogics.systemEventsLM;
        if(getSyntax().getSyntaxType() == SQLSyntaxType.MSSQL)
            Expr.useCasesCount = 5;

        try {
            startLog("Synchronizing DB");
            synchronizeDB();
        } catch (Exception e) {
            throw new RuntimeException("Error starting DBManager: ", e);
        }
    }

    @IdentityStrongLazy // ресурсы потребляет
    private SQLSession getIDSql() { // подразумевает synchronized использование
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @IdentityStrongLazy
    public SQLSession getStopSql() {
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getSystemUser() {
        return systemUser;
    }

    public long getServerComputer() {
        return serverComputer;
    }

    public SQLSessionContextProvider contextProvider = new SQLSessionContextProvider() {
        @Override
        public Long getCurrentUser() {
            return ThreadLocalContext.getCurrentUser();
        }

        @Override
        public String getCurrentAuthToken() {
            return null;
        }

        @Override
        public LogInfo getLogInfo() {
            return LogInfo.system;
        }

        @Override
        public Long getCurrentComputer() {
            return ThreadLocalContext.getCurrentComputer();
        }
        
        public Long getCurrentConnection() {
            return ThreadLocalContext.getCurrentConnection();
        }

        public Long getThreadCurrentUser() {
            return getSystemUser();
        }

        public Long getThreadCurrentComputer() {
            return getServerComputer();
        }

        @Override
        public LocalePreferences getLocalePreferences() {
            return null;
        }
    };

    public IsServerRestartingController getIsServerRestartingController() {
        return () -> restartManager.isPendingRestart();
    }

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return createSQL(contextProvider);
    }

    public SQLSession createSQL(SQLSessionContextProvider environment) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return new SQLSession(adapter, environment);
    }

    public SQLSession getThreadLocalSql() throws SQLException {
        SQLSession sqlSession = threadLocalSql.get();
        if(sqlSession == null) {
            try {
                sqlSession = createSQL();
                threadLocalSql.set(sqlSession);
            } catch (Throwable t) {
                throw ExceptionUtils.propagate(t, SQLException.class);
            }
        }
        return sqlSession;
    }

    public SQLSession closeThreadLocalSql() {
        SQLSession sql = threadLocalSql.get();
        if(sql!=null)
            try {
                sql.close();
            } catch (SQLException e) {
                ServerLoggers.sqlSuppLog(e);
            } finally {
                threadLocalSql.set(null);
            }
        return sql;
    }

    public long generateID() {
        try {
            return IDTable.instance.generateID(getIDSql(), IDTable.OBJECT);
        } catch (SQLException e) {
            throw new RuntimeException(localize("{logics.info.error.reading.user.data}"), e);
        }
    }

    public DataSession createSession() throws SQLException {
        return createSession((OperationOwner)null);
    }

    public DataSession createRecalculateSession(SQLSession sql) throws SQLException { // when called from multi-thread recalculates
        // I'm not sure that we don't have to use sql parameter, but with threadLocalSql it works just fine
        return createSession();
    }

    @Deprecated
    public DataSession createSession(SQLSession sql) throws SQLException {
        return createSession(sql, null);
    }

    public DataSession createSession(OperationOwner upOwner) throws SQLException {
        return createSession(getThreadLocalSql(), upOwner);
    }

    private void setUserLoggableProperties(SQLSession sql) throws SQLException, SQLHandledException {
        Map<String, String> changes = businessLogics.getDbManager().getPropertyCNChanges(sql);

        Integer maxStatsProperty = null;
        try {
            maxStatsProperty = (Integer) reflectionLM.maxStatsProperty.read(sql, Property.defaultModifier, changesController, DataSession.emptyEnv(OperationOwner.unknown));
        } catch (Exception ignored) {
        }

        LP<PropertyInterface> isProperty = LM.is(reflectionLM.property);
        ImRevMap<PropertyInterface, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.addProperty("overStatsProperty", reflectionLM.overStatsProperty.getExpr(key));
        query.and(reflectionLM.userLoggableProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(sql, OperationOwner.unknown);

        for (ImMap<Object, Object> values : result.valueIt()) {
            String canonicalName = values.get("CNProperty").toString().trim();
            if (changes.containsKey(canonicalName)) {
                canonicalName = changes.get(canonicalName);
            }
            LP<?> lcp = null;
            try {
                lcp = businessLogics.findProperty(canonicalName);
            } catch (Exception ignored) {
            }
            if(lcp != null) { // temporary for migration, так как могут на действиях стоять
                Integer statsProperty = null;
                if(lcp.property instanceof AggregateProperty) {
                    statsProperty = (Integer) values.get("overStatsProperty");
                    if (statsProperty == null)
                        statsProperty = getPropertyInterfaceStat(lcp.property);
                }
                if (statsProperty == null || maxStatsProperty == null || statsProperty < maxStatsProperty) {
                    lcp.makeUserLoggable(LM, systemEventsLM, getNamingPolicy());
                }
            }
        }
    }

    private void setUserMaterializedProperties(SQLSession sql) throws SQLException, SQLHandledException {
        ImRevMap<Object, KeyExpr> keys = LM.is(reflectionLM.property).getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.addProperty("materialized", reflectionLM.userMaterializedProperty.getExpr(key));
        query.and(reflectionLM.userMaterializedProperty.getExpr(key).getWhere());

        for (ImMap<Object, Object> values : query.execute(sql, OperationOwner.unknown).valueIt()) {
            LP<?> prop = businessLogics.findProperty(values.get("CNProperty").toString().trim());
            if (prop != null && !(prop.property instanceof StoredDataProperty)) {
                Boolean materialized = (Boolean) values.get("materialized");
                if (materialized) {
                    if (!prop.property.isMarkedStored()) {
                        LM.materialize(prop, namingPolicy);
                    }
                } else {
                    if (prop.property.isMarkedStored()) {
                        LM.dematerialize(prop, namingPolicy);
                    }
                }
            }
        }
    }

    private void setUserMaterializedProperties(SQLSession sql) throws SQLException, SQLHandledException {
        ImRevMap<Object, KeyExpr> keys = LM.is(reflectionLM.property).getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.addProperty("materialized", reflectionLM.userMaterializedProperty.getExpr(key));
        query.and(reflectionLM.userMaterializedProperty.getExpr(key).getWhere());

        for (ImMap<Object, Object> values : query.execute(sql, OperationOwner.unknown).valueIt()) {
            LP<?> prop = businessLogics.findProperty(values.get("CNProperty").toString().trim());
            if(prop != null) {
                Boolean materialized = (Boolean) values.get("materialized");
                if (materialized) {
                    if (!prop.property.isMarkedStored()) {
                        LM.materialize(prop, namingPolicy);
                    }
                } else {
                    if (prop.property.isMarkedStored()) {
                        LM.dematerialize(prop, namingPolicy);
                    }
                }
            }
        }
    }

    public DataSession createSession(SQLSession sql, UserController userController, FormController formController,
                                     TimeoutController timeoutController, ChangesController changesController, LocaleController localeController, IsServerRestartingController isServerRestartingController, OperationOwner owner) throws SQLException {
        return new DataSession(sql, userController, formController, timeoutController, changesController, localeController, isServerRestartingController,
                LM.baseClass, businessLogics.systemEventsLM.session, businessLogics.systemEventsLM.currentSession, getIDSql(), businessLogics, owner, null);
    }


    public boolean isServer() {
        try {
            String localhostName = SystemUtils.getLocalHostName();
            return DBManager.HOSTNAME_COMPUTER == null || (localhostName != null && localhostName.equals(DBManager.HOSTNAME_COMPUTER));
        } catch (Exception e) {
            logger.error("Error reading computer: ", e);
            throw new RuntimeException(e);
        }
    }

    // gets or adds computer
    public DataObject getComputer(String strHostName, DataSession session, ExecutionStack stack) {
        try {
            ObjectValue result = businessLogics.authenticationLM.computerHostname.readClasses(session, new DataObject(strHostName));
            if (!(result instanceof DataObject)) {
                DataObject addObject = session.addObject(businessLogics.authenticationLM.computer);
                businessLogics.authenticationLM.hostnameComputer.change(strHostName, session, addObject);
                apply(session, stack);
                return new DataObject((Long)addObject.object, businessLogics.authenticationLM.computer); // to update classes after apply
            }

            logger.debug("Begin user session " + strHostName + " " + result);
            return (DataObject) result;
        } catch (Exception e) {
            logger.error("Error reading computer: ", e);
            throw new RuntimeException(e);
        }
    }

    private String getDroppedTablesString(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException, SQLHandledException {
        String droppedTables = "";
        for (DBTable table : oldDBStructure.tables.keySet()) {
            if (newDBStructure.getTable(table.getName()) == null) {
                ImRevMap<KeyField, KeyExpr> mapKeys = table.getMapKeys();
                Expr expr = GroupExpr.create(MapFact.<KeyField, KeyExpr>EMPTY(), ValueExpr.COUNT, table.join(mapKeys).getWhere(), GroupType.SUM, MapFact.EMPTY());
                Object result = Expr.readValue(sql, expr, OperationOwner.unknown); // таблица не пустая
                if (result != null) {
                    if (!droppedTables.equals("")) {
                        droppedTables += ", ";
                    }
                    droppedTables += table;
                }
            }
        }
        return droppedTables;
    }

    // dropping caches when there are changes depending on inputListProperties
    public static class Param<P extends PropertyInterface> {
        public final ImMap<P, ObjectValue> mapValues;
        public final ImMap<CurrentEnvironmentProperty, Object> envValues; // can have null values
        public final ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders; // it's not pretty clean solution, but it's the easiest way to implement this
        public final String value;
        public final int neededCount;
        public final Object mode;

        public Param(ImMap<P, ObjectValue> mapValues, ImMap<CurrentEnvironmentProperty, Object> envValues, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, String value, int neededCount, Object mode) {
            this.mapValues = mapValues;
            this.envValues = envValues;
            this.orders = orders;
            this.value = value;
            this.neededCount = neededCount;
            this.mode = mode;
        }

        private static <P extends PropertyInterface> boolean equalsMap(ImOrderMap<PropertyInterfaceImplement<P>, Boolean> ordersA, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> ordersB) {
            if(ordersA.size() != ordersB.size())
                return false;

            for(int i = 0, size = ordersA.size(); i < size; i++) {
                if(!(ordersA.getKey(i).equalsMap(ordersB.getKey(i)) && ordersA.getValue(i).equals(ordersB.getValue(i))))
                    return false;
            }

            return true;
        }

        private static <P extends PropertyInterface> int hashMap(ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders) {
            int hashCode = 1;
            for (int i=0,size=orders.size();i<size;i++)
                hashCode = 31 * hashCode + (orders.getKey(i).hashMap() ^ BaseUtils.nullHash(orders.getValue(i)));
            return hashCode;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof Param && mapValues.equals(((Param) o).mapValues) && envValues.equals(((Param) o).envValues) && value.equals(((Param) o).value) && neededCount == ((Param<?>) o).neededCount && mode == ((Param) o).mode && equalsMap(orders, ((Param<P>) o).orders);
        }

        public int hashCode() {
            return 31 * (31 * (31 * ( 31 * (31 * mapValues.hashCode() + envValues.hashCode())  + value.hashCode()) + mode.hashCode()) + neededCount) + hashMap(orders);
        }
    }
    // basically there are 2 strategies:
    // weak "lazy", like in getAsyncValues
    // strong "lazy" - with the events in the database
    // the first is good for really rarely changed props, the second has more overhead
    private final WeakFlushedCache<ImMap<?, ? extends ObjectValue>, ObjectValue> weakLazyValueCache = new WeakFlushedCache<>(LRUUtil.G1);
    private final StrongFlushedCache<ImMap<?, ? extends ObjectValue>, ObjectValue> strongLazyValueCache = new StrongFlushedCache<>(LRUUtil.G1);
    private final WeakFlushedCache<Param, PropertyAsync[]> asyncValuesCache1 = new WeakFlushedCache<>(LRUUtil.G1);

    public DataSession createSession(SQLSession sql, OperationOwner upOwner) throws SQLException {
        return createSession(sql,
                new UserController() {
                    public boolean changeCurrentUser(DataObject user, ExecutionStack stack) {
                        throw new RuntimeException("not supported");
                    }

                    @Override
                    public Long getCurrentUserRole() {
                        return null;
                    }
                },
                new FormController() {
                    @Override
                    public void changeCurrentForm(String form) {
                        throw new RuntimeException("not supported");
                    }

                    @Override
                    public String getCurrentForm() {
                        return null;
                    }
                },
                () -> 0, changesController, Locale::getDefault, getIsServerRestartingController(), upOwner
        );
    }
    private final WeakFlushedCache<Param, PropertyAsync[]> asyncValuesCache2 = new WeakFlushedCache<>(LRUUtil.G2);

    public <T extends PropertyInterface> ObjectValue readLazyValue(Property<T> property, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return (property.isLazyStrong() ? strongLazyValueCache : weakLazyValueCache).proceed(property, keys,
                () -> property.readClasses(getThreadLocalSql(), keys, LM.baseClass, Property.defaultModifier, DataSession.emptyEnv(OperationOwner.unknown)));
    }

//    public <T extends PropertyInterface> void updateLazyValue(Property<T> property, ImMap<T, ? extends ObjectValue> values) {
//
//    }

    public <P extends PropertyInterface> PropertyAsync<P>[] getAsyncValues(InputValueList<P> list, QueryEnvironment env, ExecutionStack stack, FormEnvironment formEnv, String value, int neededCount, AsyncMode mode) throws SQLException, SQLHandledException {
        if(Settings.get().isIsClustered()) // we don't want to use caches since they can be inconsistent
            return readAsyncValues(list, env, stack, formEnv, value, neededCount, mode);

        return (value.length() >= Settings.get().getAsyncValuesLongCacheThreshold() || list.hasValues() ? asyncValuesCache1 : asyncValuesCache2).
                proceed(list.getCacheKey(), list.getCacheParam(value, neededCount, mode, env),
                () -> readAsyncValues(list, env, stack, formEnv, value, neededCount, mode));
    }

    public void flushChanges() {
        ImSet<Property> flushedChanges;
        synchronized (changesListLock) {
            flushedChanges = mChanges.immutable();
            mChanges = SetFact.mSet();
        }
        if(flushedChanges.isEmpty())
            return;

        asyncValuesCache1.flush(flushedChanges);
        asyncValuesCache2.flush(flushedChanges);
        weakLazyValueCache.flush(flushedChanges);
    }

    public void flushStrong(ImSet<Pair<Property, ImMap<PropertyInterface, ? extends ObjectValue>>> strongCaches) {
        for (int i = 0, size = strongCaches.size(); i < size; i++) {
            Pair<Property, ImMap<PropertyInterface, ? extends ObjectValue>> entry = strongCaches.get(i);
            strongLazyValueCache.flush(entry.first, entry.second);
        }
    }

    private static class ParamRef<K> {
        public K param;

        public ParamRef(K param) {
            this.param = param;
        }

        private void drop() {
            param = null;
        }
    }

    private static class ValueRef<K, V> {
        // it's the only strong value to keep it from garbage collected
        public final ParamRef<K> ref;
        public final V values;

        public ValueRef(ParamRef<K> ref, V values) {
            this.ref = ref;
            this.values = values;
        }
    }

    private static class StrongValueRef<V> {
        public V values;
        public boolean obsolete;
        public boolean finished;

        public StrongValueRef() {
        }
    }

    private <P extends PropertyInterface> PropertyAsync<P>[] readAsyncValues(InputValueList<P> list, QueryEnvironment env, ExecutionStack stack, FormEnvironment formEnv, String value, int neededCount, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        return FormInstance.getAsyncValues(list, getThreadLocalSql(), env, LM.baseClass, Property.defaultModifier, () -> new FormInstance.ExecContext() {
            private DataSession session;

            public ExecutionContext<?> getContext() throws SQLException {
                session = createSession();
                return new ExecutionContext<>(MapFact.EMPTY(), createSession(), stack, formEnv);
            }

            public void close() throws SQLException {
                session.close();
            }
        } , value, neededCount, asyncMode);
    }

    private interface FlushedCache<K,V> {
        V proceed(ActionOrProperty property, K param, SQLCallable<V> function) throws SQLException, SQLHandledException;
    }

    // need this to clean outdated caches
    // could be done easier with timestamps (value and last changed), but this way cache will be cleaned faster
    private static class WeakFlushedCache<K, V> implements FlushedCache<K, V> {
        private final LRUWVSMap<ActionOrProperty<?>, ConcurrentIdentityWeakHashSet<ParamRef<K>>> propCache;
        private final LRUWWEVSMap<ActionOrProperty<?>, K, ValueRef<K, V>> valueCache;

        public WeakFlushedCache(LRUUtil.Strategy expireStrategy) {
            propCache = new LRUWVSMap<>(expireStrategy);
            valueCache = new LRUWWEVSMap<>(expireStrategy);
        }

        public V proceed(ActionOrProperty property, K param, SQLCallable<V> function) throws SQLException, SQLHandledException {
            ValueRef<K, V> valueRef = valueCache.get(property, param);
            if(valueRef != null && valueRef.ref.param != null)
                return valueRef.values;

            // has to be before reading to be sure that ref will be dropped when changes are made
            ParamRef<K> ref = new ParamRef<>(param);
            ConcurrentIdentityWeakHashSet<ParamRef<K>> paramRefs = propCache.get(property);
            if(paramRefs == null) {
                paramRefs = new ConcurrentIdentityWeakHashSet<>();
                propCache.put(property, paramRefs);
            }
            paramRefs.add(ref);

            V values = function.call();
            valueCache.put(property, param, new ValueRef<>(ref, values));

            return values;
        }

        public void flush(ImSet<Property> flushedChanges) {
            propCache.proceedSafeLockLRUEKeyValues((property, refs) -> {
                if (property != null && InputValueList.depends(property, flushedChanges)) {
                    for (ParamRef ref : refs)
                        ref.drop(); // dropping ref will eventually lead to garbage collection of entries in all lru caches, plus there is a check that cache is dropped
                }
            });
        }
    }

    private static class StrongFlushedCache<K, V> implements FlushedCache<K, V> {
        private final LRUWWEVSMap<ActionOrProperty<?>, K, StrongValueRef<V>> valueCache;

        public StrongFlushedCache(LRUUtil.Strategy expireStrategy) {
            valueCache = new LRUWWEVSMap<>(expireStrategy);
        }

        public V proceed(ActionOrProperty property, K param, SQLCallable<V> function) throws SQLException, SQLHandledException {
            StrongValueRef<V> valueRef = valueCache.get(property, param);
            if(valueRef != null && !valueRef.obsolete && valueRef.finished)
                return valueRef.values;

            valueRef = new StrongValueRef<>();
            valueCache.put(property, param, valueRef);

            valueRef.values = function.call();
            valueRef.finished = true;

            return valueRef.values;
        }

        public void flush(ActionOrProperty property, K keys) {
            StrongValueRef<V> strongValueRef = valueCache.get(property, keys);
            if (strongValueRef != null) {
                strongValueRef.obsolete = true;
            }
        }
    }

    private final Object changesListLock = new Object();
    private MSet<Property> mChanges = SetFact.mSet();

    public void registerChange(ImSet<Property> properties) {
        synchronized (changesListLock) {
            mChanges.addAll(properties);
        }
    }

    private static ImMap<String, ImRevMap<String, String>> getFieldToCNMaps(DBStructure<?> dbStructure) {
        return SetFact.fromJavaOrderSet(dbStructure.storedProperties)
                .getSet()
                .group(value -> value.tableName)
                .mapValues(value -> value.mapRevKeyValues(DBStoredProperty::getDBName, DBStoredProperty::getCanonicalName));
    }

    // Удаляем несуществующие индексы и убираем из newDBStructure не изменившиеся индексы.
    // Делаем это до применения migration script, то есть не пытаемся сохранить все возможные индексы по максимуму
    private void checkIndexes(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {
        startLog("Checking indexes");
        ImMap<String, String> propertyCNChanges = getStoredOldToNewCNMapping(oldDBStructure, newDBStructure);

        ImMap<String, ImRevMap<String, String>> oldTableFieldToCN = getFieldToCNMaps(oldDBStructure);
        ImMap<String, ImRevMap<String, String>> newTableFieldToCN = getFieldToCNMaps(newDBStructure);

        Set<String> oldTableDBNames = new HashSet<>();
        for (DBTable table : oldDBStructure.tables.keySet()) {
            oldTableDBNames.add(table.getName());
        }

        for (Map.Entry<DBTable, List<IndexData<String>>> oldTableIndexes : oldDBStructure.tables.entrySet()) {
            DBTable oldTable = oldTableIndexes.getKey();
            List<IndexData<String>> oldIndexes = oldTableIndexes.getValue();
            DBTable newTable = newDBStructure.getTable(oldTable.getName()); // Здесь никак не учитываем возможное изменение имени

            if (newTable == null) {
                dropTableIndexes(sql, oldTable, oldIndexes, oldTableDBNames);
                continue;
            }

            List<IndexData<Field>> newTableIndexes = newDBStructure.tables.get(newTable);
            Map<Pair<List<String>, IndexType>, IndexData<Field>> newTableIndexesNames = getTableIndexNames(newTableIndexes);

            Map<String, String> fieldsOldToNew = getOldToNewFieldsMap(oldTableFieldToCN.get(oldTable.getName()),
                                                                        newTableFieldToCN.get(newTable.getName()),
                                                                        propertyCNChanges);
            for (IndexData<String> oldIndex : oldIndexes) {
                List<String> oldIndexKeys = new ArrayList<>(oldIndex.fields); // делаем копию, потому что будем изменять
                IndexOptions oldOptions = oldIndex.options;
                ImOrderSet<String> oldIndexKeysSet = SetFact.fromJavaOrderSet(oldIndex.fields);

                ReplaceResult res = replaceIndexKeys(oldIndexKeys, fieldsOldToNew, newTable.getTableKeys().toJavaSet());

                IndexData<Field> newIndex = newTableIndexesNames.get(new Pair<>(oldIndexKeys, oldOptions.type));
                if (res != ReplaceResult.FAILED && newIndex != null && newIndex.options.equalsWithoutDBName(oldOptions)) {
                    IndexOptions newOptions = newIndex.options;
                    newTableIndexes.remove(newIndex); // удаляем, чтобы в дальнейшем не создавать этот индекс
                    if (!BaseUtils.nullEquals(newOptions.dbName, oldOptions.dbName) || oldOptions.dbName == null && res == ReplaceResult.REPLACED) {
                        ImOrderSet<String> newIndexKeysSet = SetFact.fromJavaOrderSet(oldIndexKeys);
                        sql.renameIndex(oldTable, oldTable.keys, oldIndexKeysSet, newIndexKeysSet, oldOptions, newOptions, Settings.get().isStartServerAnyWay());
                    }
                } else {
                    String indexDBName = namingPolicy.transformIndexNameToDBName(sql.getOldIndexName(oldTable, oldIndex));
                    // Backward compatibility check, issue: https://github.com/lsfusion/platform/issues/1401
                    if (!oldTableDBNames.contains(indexDBName)) {
                        sql.dropIndex(oldTable, oldTable.keys, oldIndexKeysSet, oldOptions, Settings.get().isStartServerAnyWay());
                    }
                }
            }
        }
    }

    private enum ReplaceResult {FAILED, REPLACED, OK}

    private ReplaceResult replaceIndexKeys(List<String> oldIndexKeys, Map<String, String> fieldsOldToNew, Set<KeyField> tableKeys) {
        ReplaceResult res = ReplaceResult.OK;
        Set<String> tableKeyNames = tableKeys.stream().map(Field::getName).collect(Collectors.toSet());
        for (int i = 0; i < oldIndexKeys.size(); ++i) {
            String oldKey = oldIndexKeys.get(i);
            if (!tableKeyNames.contains(oldKey)) {
                String newKey = fieldsOldToNew.get(oldKey);
                if (newKey == null) {
                    return ReplaceResult.FAILED;
                }
                if (!newKey.equals(oldKey)) {
                    oldIndexKeys.set(i, newKey);
                    res = ReplaceResult.REPLACED;
                }
            }
        }
        return res;
    }

    private ImMap<String, String> getStoredOldToNewCNMapping(OldDBStructure oldDBStructure, NewDBStructure newDBStructure) {
        Map<String, String> changes = migrationManager.getStoredPropertyCNChangesAfter(oldDBStructure.migrationVersion);
        Set<String> newCanonicalNames = newDBStructure.storedProperties.stream().map(DBStoredProperty::getCanonicalName).collect(Collectors.toSet());
        Set<String> oldCanonicalNames = oldDBStructure.storedProperties.stream().map(DBStoredProperty::getCanonicalName).collect(Collectors.toSet());
        Map<String, String> result = new HashMap<>();
        Set<String> usedNewCNs = new HashSet<>();

        changes.forEach((oldCN, newCN) -> {
            if (oldCanonicalNames.contains(oldCN)) {
                result.put(oldCN, newCN);
                usedNewCNs.add(newCN);
            }
        });

        for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
            String cn = oldProperty.getCanonicalName();
            if (!result.containsKey(cn) && newCanonicalNames.contains(cn) && !usedNewCNs.contains(cn)) {
                result.put(cn, cn);
            }
        }
        return MapFact.fromJavaMap(result);
    }

    private void dropTableIndexes(SQLSession sql, DBTable table, List<IndexData<String>> indexes, Set<String> oldTableDBNames) throws SQLException {
        for (IndexData<String> index : indexes) {
            String indexDBName = namingPolicy.transformIndexNameToDBName(sql.getOldIndexName(table, index));
            // Backward compatibility check, issue: https://github.com/lsfusion/platform/issues/1401
            if (!oldTableDBNames.contains(indexDBName)) {
                ImOrderSet<String> indexKeysSet = SetFact.fromJavaOrderSet(index.fields);
                sql.dropIndex(table, table.keys, indexKeysSet, index.options, Settings.get().isStartServerAnyWay());
            }
        }
    }

    // old field -> old cn -> new cn -> new field
    private Map<String, String> getOldToNewFieldsMap(ImRevMap<String, String> oldFieldToCN,
                                                       ImRevMap<String, String> newFieldToCN,
                                                       ImMap<String, String> propertyCNChanges) {
        if (oldFieldToCN == null || newFieldToCN == null) { // tables can be empty
            return new HashMap<>();
        }

        Map<String, String> result = oldFieldToCN.innerJoin(propertyCNChanges).innerCrossValues(newFieldToCN).toJavaMap();
        // class property canonical name may differ between server starts due to non-determinism in canonical name creation
        for (int i = 0; i < oldFieldToCN.size(); ++i) {
            String oldFieldName = oldFieldToCN.getKey(i);
            if (!result.containsKey(oldFieldName)) {
                String oldCN = oldFieldToCN.getValue(i);
                if (isClassPropertyCN(oldCN) && newFieldToCN.containsKey(oldFieldName)) {
                    result.put(oldFieldName, oldFieldName);
                }
            }
        }
        return result;
    }

    private boolean isClassPropertyCN(String canonicalName) {
        String propertyName = PropertyCanonicalNameParser.getName(canonicalName);
        return propertyName.startsWith(PropertyCanonicalNameUtils.classDataPropPrefix);
    }

    private Map<Pair<List<String>, IndexType>, IndexData<Field>> getTableIndexNames(List<IndexData<Field>> newTableIndexes) {
        Map<Pair<List<String>, IndexType>, IndexData<Field>> newTableIndexesNames = new HashMap<>();

        newTableIndexes.forEach(data -> {
            List<String> names = new ArrayList<>();
            for (Field field : data.fields) {
                names.add(field.getName());
            }
            newTableIndexesNames.put(new Pair<>(names, data.options.type), data);
        });
        return newTableIndexesNames;
    }

    private void checkUniquePropertyDBName(NewDBStructure struct) {
        Map<Pair<String, String>, DBStoredProperty> sids = new HashMap<>();
        for (DBStoredProperty property : struct.storedProperties) {
            Pair<String, String> key = new Pair<>(property.getDBName(), property.getTable().getName());
            if (sids.containsKey(key)) {
                startLogError(String.format("Equal sid '%s' in table '%s': %s and %s", key.first, key.second, sids.get(key).getCanonicalName(), property.getCanonicalName()));
            }
            sids.put(key, property);
         }
    }

    public static <P extends PropertyInterface> Where getIncorrectWhere(ImplementTable table, BaseClass baseClass, final ImRevMap<KeyField, KeyExpr> mapKeys) {
        final Where inTable = baseClass.getInconsistentTable(table).join(mapKeys).getWhere();
        Where correctClasses = table.getClasses().getWhere(mapKeys, true, IsClassType.INCONSISTENT);
        return inTable.and(correctClasses.not());
    }

    @StackMessage("{logics.upload.db}")
    private void uploadTableToDB(SQLSession sql, final @ParamMessage DBTable implementTable, @ParamMessage String progress, final SQLSession sqlTo, final OperationOwner owner) throws SQLException, SQLHandledException {
        sqlTo.truncate(implementTable, owner);

        final ProgressStackItemResult stackItem = new ProgressStackItemResult();
        try {
            final Result<Integer> proceeded = new Result<>(0);
            final int total = sql.getCount(implementTable, owner);
            ResultHandler<KeyField, PropertyField> reader = new ReadBatchResultHandler<KeyField, PropertyField>(10000) {
                public void start() {
                    stackItem.value = ExecutionStackAspect.pushProgressStackItem(localize("{logics.upload.db}"), proceeded.result, total);
                }

                public void proceedBatch(ImOrderMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> batch) throws SQLException {
                    sqlTo.insertBatchRecords(implementTable, batch.getMap(), owner);
                    proceeded.set(proceeded.result + batch.size());
                    ExecutionStackAspect.popProgressStackItem(stackItem.value);
                    stackItem.value = ExecutionStackAspect.pushProgressStackItem(localize("{logics.upload.db}"), proceeded.result, total);
                }

                public void finish() throws SQLException {
                    ExecutionStackAspect.popProgressStackItem(stackItem.value);
                    super.finish();
                }
            };
            implementTable.readData(sql, LM.baseClass, owner, true, reader);
        } finally {
            ExecutionStackAspect.popProgressStackItem(stackItem.value);
        }
    }

    private class ProgressStackItemResult {
        ProgressStackItem value;
    }

    private OldDBStructure getOldDBStructure(SQLSession sql) throws SQLException, SQLHandledException, IOException {
        DataInputStream inputDB = null;
        StructTable structTable = StructTable.instance;
        RawFileData struct = (RawFileData) sql.readRecord(structTable, MapFact.EMPTY(), structTable.struct, OperationOwner.unknown);
        if (struct != null) {
            inputDB = new DataInputStream(struct.getInputStream());
        }
        return new OldDBStructure(inputDB);
    }

    public static class IDAdd {
        public final long object;

        public final ConcreteCustomClass customClass;
        public final String sID;
        public final String caption;
        public final String image;

        public IDAdd(long object, ConcreteCustomClass customClass, String sID, String caption, String image) {
            this.object = object;
            this.customClass = customClass;
            this.sID = sID;
            this.caption = caption;
            this.image = image;
        }
    }

    public static class IDRemove {
        public final DataObject object;

        public final String sID; // SID value only for logging

        public IDRemove(DataObject object, String sID) {
            this.object = object;
            this.sID = sID;
        }
    }

    public static class IDChanges {

        public final List<IDAdd> added = new ArrayList<>();
        public final List<IDRemove> removed = new ArrayList<>();

        public final Map<DataObject, String> modifiedSIDs = new HashMap<>();
        public final Map<DataObject, String> modifiedCaptions = new HashMap<>();
        public final Map<DataObject, String> modifiedImages = new HashMap<>();

        public void apply(DataSession session, BaseLogicsModule LM, boolean isFirstStart) throws SQLException, SQLHandledException {
            LM.fillingIDs.change(true, session); // need this to avoid constraint on staticName changing

            for (IDAdd addedObject : added) {
                if(!isFirstStart)
                    startLog("Adding static object with id " + addedObject.object + ", sid " + addedObject.sID + ", name " + addedObject.caption + ", image " + addedObject.image);
                DataObject classObject = new DataObject(addedObject.object, LM.baseClass.unknown);
                session.changeClass(classObject, addedObject.customClass);
                LM.staticName.change(addedObject.sID, session, classObject);
                LM.staticCaption.change(addedObject.caption, session, classObject);
                LM.staticImage.change(addedObject.image, session, classObject);
            }

            for (Map.Entry<DataObject, String> modifiedSID : modifiedSIDs.entrySet()) {
                startLog("Changing sid of static object with id " + modifiedSID.getKey() + " to " + modifiedSID.getValue());
                LM.staticName.change(modifiedSID.getValue(), session, modifiedSID.getKey());
            }

            for (Map.Entry<DataObject, String> modifiedCaption : modifiedCaptions.entrySet()) {
                startLog("Renaming static object with id " + modifiedCaption.getKey() + " to " + modifiedCaption.getValue());
                LM.staticCaption.change(modifiedCaption.getValue(), session, modifiedCaption.getKey());
            }

            for (Map.Entry<DataObject, String> modifiedImage : modifiedImages.entrySet()) {
                startLog("Changing image of static object with id " + modifiedImage.getKey() + " to " + modifiedImage.getValue());
                LM.staticImage.change(modifiedImage.getValue(), session, modifiedImage.getKey());
            }

            for (IDRemove removedObject : removed) {
                startLog("Removing static object with id " + removedObject.object.object + " and sid " + removedObject.sID);
                session.changeClass(removedObject.object, LM.baseClass.unknown);
            }
        }
    }

    public void synchronizeDB() throws Exception {

        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // "старое" состояние базы
        OldDBStructure oldDBStructure = getOldDBStructure(sql);
        boolean isFirstStart = oldDBStructure.isEmpty();
        if(!isFirstStart && oldDBStructure.version < 30)
            throw new RuntimeException("You should update to version 30 first");
        if (!isFirstStart && oldDBStructure.version < 37) {
            createAdditionalIndexRecords(oldDBStructure);
        }
        checkModules(oldDBStructure);

        // В этот момент в обычной ситуации migration script уже был обработан, вызов оставлен на всякий случай. Повторный вызов ничего не делает.
        runMigrationScript();
        migrationManager.checkMigrationVersion(oldDBStructure.migrationVersion);

        boolean noTransSyncDB = Settings.get().isNoTransSyncDB();

        try {
            sql.pushNoHandled();

            if(noTransSyncDB)
                sql.startFakeTransaction(OperationOwner.unknown);
            else
                sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);

            ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
            DataOutputStream outDB = new DataOutputStream(outDBStruct);

            MigrationVersion newMigrationVersion = migrationManager.getCurrentMigrationVersion(oldDBStructure.migrationVersion);
            NewDBStructure newDBStructure = new NewDBStructure(newMigrationVersion, sql);

            checkUniquePropertyDBName(newDBStructure);
            newDBStructure.write(outDB);

            // DROP / RENAME indices
            checkIndexes(sql, oldDBStructure, newDBStructure);

            if (!isFirstStart)
                alterDBStructure(sql, oldDBStructure, newDBStructure);

            // CREATE / CHANGE TYPES tables (keys)

            createTables(sql, oldDBStructure, newDBStructure);

            changeKeyTypes(sql, oldDBStructure, newDBStructure);

            // BUILDING DIFF properties and objects

            List<DBStoredProperty> createProperties = new LinkedList<>();
            Map<ImplementTable, Map<Field, Type>> changePropertyTypes = new HashMap<>();
            List<MoveDBProperty> moveProperties = new ArrayList<>();
            List<DBStoredProperty> dropProperties = new ArrayList<>();
            buildPropertiesDiff(oldDBStructure, newDBStructure, createProperties, changePropertyTypes, moveProperties, dropProperties);

            ImMap<String, ImMap<String, ImSet<Long>>> movedObjects = buildObjectsDiff(oldDBStructure, newDBStructure);

            // CREATE / CHANGE TYPE properties

            createColumns(sql, createProperties);

            changeColumnTypes(sql, changePropertyTypes);

            // since the below methods use queries we have to update stat props first
            ImplementTable.reflectionStatProps(() -> {
                startLog("Updating stats");
                updateStats(sql, true);
                return null;
            });
            ImplementTable.updatedStats = true;

            // since dropping tables uses queries we need to do it after updating stats
            if (isDenyDropTables()) {
                String droppedTables = getDroppedTablesString(sql, oldDBStructure, newDBStructure);
                if (!droppedTables.isEmpty()) {
                    throw new RuntimeException("Dropping tables: " + droppedTables + "\nNow, dropping tables is restricted by settings. If you are sure you want to drop these tables, you can set 'db.denyDropTables = false'\nin settings.properties or through other methods. For more information, please visit: https://docs.lsfusion.org/Launch_parameters/#applsfusion");
                }
            }

            // MOVE properties / objects (both uses query, should be before table drop)

            moveColumns(sql, oldDBStructure, moveProperties);

            moveObjects(sql, oldDBStructure, newDBStructure, movedObjects, LM.baseClass); // should be before tables and columns drop (since class data props are also dropped)

            // CREATE indexes (need it after moving the columns, because they are created there)

            createIndexes(sql, oldDBStructure, newDBStructure);

            // DROP properties

            Map<String, String> dropDataProperties = new HashMap<>();
            dropColumns(sql, dropProperties, dropDataProperties);

            // DROP / PACK tables

            dropTables(sql, oldDBStructure, newDBStructure);

            packTables(sql, oldDBStructure, newDBStructure, dropProperties, movedObjects);

            startLog("Filling static objects ids");
            IDChanges idChanges = new IDChanges();
            LM.baseClass.fillIDs(sql, DataSession.emptyEnv(OperationOwner.unknown), this::generateID, LM.staticCaption, LM.staticImage,LM.staticName,
                    migrationManager.getClassSIDChangesAfter(oldDBStructure.migrationVersion),
                    migrationManager.getObjectSIDChangesAfter(oldDBStructure.migrationVersion),
                    idChanges, changesController);

            for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                newClass.ID = newClass.customClass.ID;
            }

            // we need after fillIDs because isValueUnique / usePrev can call classExpr -> getClassObject which uses ID
            new TaskRunner(getBusinessLogics()).runTask(initTask);

            try (DataSession session = createSession(OperationOwner.unknown)) { // apply in transaction
                startLog("Writing static objects changes");
                idChanges.apply(session, LM, isFirstStart);
                apply(session);

                startLog("Migrating reflection properties and actions");
                migrateReflectionProperties(session, oldDBStructure);
                apply(session);

                newDBStructure.writeConcreteClasses(outDB);

                try {
                    sql.insertRecord(StructTable.instance, MapFact.EMPTY(), MapFact.singleton(StructTable.instance.struct, new DataObject(new RawFileData(outDBStruct), ByteArrayClass.instance)), true, TableOwner.global, OperationOwner.unknown);
                } catch (Exception e) {
                    ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(StructTable.instance.struct, new DataObject(RawFileData.EMPTY, ByteArrayClass.instance));
                    sql.insertRecord(StructTable.instance, MapFact.EMPTY(), propFields, true, TableOwner.global, OperationOwner.unknown);
                }
                apply(session);

                if (!isFirstStart) { // если все свойства "новые" то ничего перерасчитывать не надо
                    startLog("Recalculating materializations");
                    List<AggregateProperty> recalculateProperties = new ArrayList<>();
                    for (DBStoredProperty property : createProperties) {
                        if(property.property instanceof AggregateProperty) {
                            recalculateProperties.add((AggregateProperty) property.property);
                        }
                    }
                    recalculateMaterializations(session, sql, recalculateProperties, false, ServerLoggers.startLogger);
                    apply(session);

                    List<Pair<Property, Boolean>> updateStatProperties = new ArrayList<>();
                    for (DBStoredProperty property : createProperties)
                        updateStatProperties.add(new Pair<>(property.property, property.property instanceof StoredDataProperty)); // we don't want to update DATA stats (it is always zero)
                    for (MoveDBProperty move : moveProperties)
                        updateStatProperties.add(new Pair<>(move.newProperty.property, false));
                    updateMaterializationStats(session, updateStatProperties);
                    apply(session);
                }

                writeDroppedColumns(session, dropDataProperties);
                apply(session);
            }
            if(!noTransSyncDB)
                sql.commitTransaction();

        } catch (Throwable e) {
            if(!noTransSyncDB)
                sql.rollbackTransaction();
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        } finally {
            if(noTransSyncDB)
                sql.endFakeTransaction(OperationOwner.unknown);

            sql.popNoHandled();
        }

        // with apply outside transaction
        try (DataSession session = createSession()) {
            setDefaultUserLocalePreferences(session);
            setLogicsParams(session);

            initSystemUser(session);

            classForNameSQL();
        }
    }

    private static void packTables(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure, List<DBStoredProperty> dropProperties, ImMap<String, ImMap<String, ImSet<Long>>> movedObjects) throws SQLException, SQLHandledException {
        Set<String> packTableNames = new HashSet<>();
        for (DBStoredProperty dropColumn : dropProperties)
            packTableNames.add(dropColumn.tableName);
        for(ImMap<String, ImSet<Long>> movedOldObjects : movedObjects.valueIt())
            for(String classPropName : movedOldObjects.keyIt())
                packTableNames.add(oldDBStructure.getProperty(classPropName).tableName);

        MSet<ImplementTable> mPackTables = SetFact.mSet();
        for (String packTableName : packTableNames) {
            ImplementTable packTable = (ImplementTable) newDBStructure.getTable(packTableName);
            if (packTable != null)
                mPackTables.add(packTable);
        }
        packTables(sql, mPackTables.immutable(), false);
    }

    private static void moveColumns(SQLSession sql, OldDBStructure oldDBStructure, List<MoveDBProperty> movedProperties) throws Exception {
        for(MoveDBProperty move : movedProperties) {
            DBStoredProperty newProperty = move.newProperty;
            DBStoredProperty oldProperty = move.oldProperty;

            ImplementTable newTable = newProperty.getTable();
            DBTable oldTable = oldDBStructure.getTable(oldProperty.tableName);

            sql.addColumn(newTable, newProperty.property.field, false);
            runWithStartLog(() -> newTable.moveColumn(sql, newProperty.property.field, oldTable, move.mapKeys, oldTable.findProperty(oldProperty.getDBName())),
                    localize(LocalizedString.createFormatted("{logics.info.property.transferring.from.table.to.table}", newProperty.property.field.toString(), newProperty.property.caption, oldProperty.tableName, newProperty.tableName)));

            sql.dropColumn(oldProperty.getTableName(sql.syntax), oldProperty.getDBName(), Settings.get().isStartServerAnyWay());
        }
    }

    private static void changeColumnTypes(SQLSession sql, Map<ImplementTable, Map<Field, Type>> changedTypeProperties) throws SQLException {
        for (Map.Entry<ImplementTable, Map<Field, Type>> entry : changedTypeProperties.entrySet()) {
            runWithStartLog(() -> sql.modifyColumns(entry.getKey(), entry.getValue()),
                    "Changing type of property columns (" + entry.getValue().size() + ") in table " + entry.getKey().getName());
        }
    }

    private static void buildPropertiesDiff(OldDBStructure oldDBStructure, NewDBStructure newDBStructure, List<DBStoredProperty> createProperties, Map<ImplementTable, Map<Field, Type>> changePropertyTypes, List<MoveDBProperty> moveProperties, List<DBStoredProperty> dropProperties) {
        createProperties.addAll(newDBStructure.storedProperties);
        for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
            boolean keep = false;
            for (Iterator<DBStoredProperty> is = createProperties.iterator(); is.hasNext(); ) {
                DBStoredProperty newProperty = is.next();

                if (newProperty.getCanonicalName().equals(oldProperty.getCanonicalName())) {
                    MRevMap<KeyField, PropertyInterface> mFoundInterfaces = MapFact.mRevMapMax(newProperty.property.interfaces.size());
                    for (PropertyInterface propertyInterface : newProperty.property.interfaces) {
                        KeyField mapKeyField = oldProperty.mapKeys.get(propertyInterface.ID);
                        if (mapKeyField != null)
                            mFoundInterfaces.revAdd(mapKeyField, propertyInterface);
                    }
                    ImRevMap<KeyField, PropertyInterface> foundInterfaces = mFoundInterfaces.immutableRev();

                    if (foundInterfaces.size() == oldProperty.mapKeys.size()) {
                        keep = true;
                        if (!newProperty.tableName.equals(oldProperty.tableName))
                            moveProperties.add(new MoveDBProperty(newProperty, oldProperty, foundInterfaces.join((ImRevMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys)));
                        else {
                            Type oldType = oldDBStructure.getTable(oldProperty.tableName).findProperty(oldProperty.getDBName()).type;
                            if (!oldType.equals(newProperty.property.field.type)) {
                                startLog("Prepare changing type of property column " + newProperty.property.field + " in table " + newProperty.tableName + " from " + oldType + " to " + newProperty.property.field.type);
                                changePropertyTypes.computeIfAbsent(newProperty.getTable(), k -> new HashMap<>()).put(newProperty.property.field, oldType);
                            }
                        }
                        is.remove();
                    }
                    break;
                }
            }
            if (!keep)
                dropProperties.add(oldProperty);
        }
    }

    private static void dropDataColumn(SQLSession sql, Map<String, String> dropDataProperties, DBStoredProperty oldProperty) throws SQLException {
        String oldName = oldProperty.getDBName();
        String newName = "_DELETED_" + oldProperty.getDBName();
        runWithStartLog(() -> sql.renameColumn(oldProperty.getTableName(sql.syntax), oldName, newName),
                "Deleting column " + oldName + " " + "(renaming to " + newName + ")  in table " + oldProperty.tableName);
        dropDataProperties.put(newName, oldProperty.tableName);
    }

    private static void createColumns(SQLSession sql, List<DBStoredProperty> restNewDBStored) throws SQLException {
        for (DBStoredProperty property : restNewDBStored) // добавляем оставшиеся
            sql.addColumn(property.getTable(), property.property.field, Settings.get().isStartServerAnyWay());
    }

    private static void changeKeyTypes(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {
        for (DBTable table : newDBStructure.tables.keySet()) {
            DBTable oldTable = oldDBStructure.getTable(table.getName());
            if (oldTable != null) {
                for (KeyField key : table.keys) {
                    KeyField oldKey = oldTable.findKey(key.getName());
                    if(oldKey == null)
                        throw new RuntimeException("Key " + key + " is not found in table : " + oldTable + ". New table : " + table);
                    if (!(key.type.equals(oldKey.type))) {
                        startLog("Changing type of key column " + key + " in table " + table + " from " + oldKey.type + " to " + key.type);
                        sql.modifyColumn(table, key, oldKey.type);
                    }
                }
            }
        }
    }

    private static void createTables(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {
        // добавим таблицы которых не было
        startLog("Creating tables");
        for (DBTable table : newDBStructure.tables.keySet()) {
            if (oldDBStructure.getTable(table.getName()) == null)
                sql.createTable(table, table.keys, Settings.get().isStartServerAnyWay());
        }
    }

    private static void createIndexes(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {
        startLog("Adding indexes");
        for (Map.Entry<DBTable, List<IndexData<Field>>> mapIndex : newDBStructure.tables.entrySet())
            for (IndexData<Field> index : mapIndex.getValue()) {
                DBTable table = mapIndex.getKey();
                addIndexWithStartLog(() ->
                    sql.addIndex(table, table.keys, SetFact.fromJavaOrderSet(index.fields), index.options, Settings.get().isStartServerAnyWay()),
                        "Adding index: " + sql.getIndexName(table, index), oldDBStructure.getTable(table.getName()) != null); // no need to log if the table is new
            }
    }

    private static void addIndexWithStartLog(E2Runnable<SQLException, SQLException> run, String message, boolean log) throws SQLException {
        if(log) {
            runWithStartLog(run, message);
        } else {
            run.run();
        }
    }

    private static void dropTables(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {
        // удаляем таблицы старые
        for (DBTable table : oldDBStructure.tables.keySet()) {
            if (newDBStructure.getTable(table.getName()) == null) {
                startLog("Dropping table " + table);
                sql.dropTable(table);
            }
        }
    }

    private static void dropColumns(SQLSession sql, List<DBStoredProperty> dropColumns, Map<String, String> dropDataProperties) throws SQLException {
        for (DBStoredProperty dropColumn : dropColumns) {
            boolean droppedData = false;
            if (dropColumn.isDataProperty) {
                ExConnection exConnection = sql.getConnection();
                Connection connection = exConnection.sql;
                Savepoint savepoint = null;
                try {
                    savepoint = connection.setSavepoint();
                    dropDataColumn(sql, dropDataProperties, dropColumn);
                    droppedData = true;
                } catch (PSQLException e) { // колонка с новым именем уже существует
                    if (savepoint != null)
                        connection.rollback(savepoint);
                } finally {
                    sql.returnConnection(exConnection, OperationOwner.unknown);
                }
            }
            if(!droppedData) {
                startLog("Dropping column " + dropColumn.getDBName() + " from table " + dropColumn.tableName);
                sql.dropColumn(dropColumn.getTableName(sql.syntax), dropColumn.getDBName(), Settings.get().isStartServerAnyWay());
            }
        }
    }

    private static void dropObjects(SQLSession sql, OldDBStructure oldDBStructure, ImMap<String, ImMap<String, ImSet<Long>>> movedObjects, BaseClass baseClass) throws SQLException, SQLHandledException {
        ImMap<String, ImSet<Long>> toClean = MapFact.mergeMaps(movedObjects.values(), ASet.addMergeSet());
        for (int i = 0, size = toClean.size(); i < size; i++) { // удалим оставшиеся классы
            DBStoredProperty classProp = oldDBStructure.getProperty(toClean.getKey(i));
            DBTable table = oldDBStructure.getTable(classProp.tableName);

            QueryBuilder<KeyField, PropertyField> dropClassObjects = new QueryBuilder<>(table);
            Where moveWhere = Where.FALSE();

            PropertyField oldField = table.findProperty(classProp.getDBName());
            Expr oldExpr = table.join(dropClassObjects.getMapExprs()).getExpr(oldField);
            for (long prevID : toClean.getValue(i))
                moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, baseClass.objectClass), Compare.EQUALS));
            dropClassObjects.addProperty(oldField, Expr.NULL());
            dropClassObjects.and(moveWhere);

            startLog(localize(LocalizedString.createFormatted("{logics.info.objects.removing.from.table}", classProp.tableName)));
            sql.updateRecords(new ModifyQuery(table, dropClassObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
        }
    }

    private static void moveObjects(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure, ImMap<String, ImMap<String, ImSet<Long>>> movedObjects, BaseClass baseClass) throws Exception {
        for (int i = 0, size = movedObjects.size(); i < size; i++) { // перенесем классы, которые сохранились, но изменили поле
            DBStoredProperty classProp = newDBStructure.getProperty(movedObjects.getKey(i));
            DBTable table = newDBStructure.getTable(classProp.tableName);

            moveObjects(table, sql, oldDBStructure, movedObjects.getValue(i), classProp, baseClass);
        }

        dropObjects(sql, oldDBStructure, movedObjects, baseClass);
    }

    private static ImMap<String, ImMap<String, ImSet<Long>>> buildObjectsDiff(OldDBStructure oldDBStructure, NewDBStructure newDBStructure) {
        MMap<String, ImMap<String, ImSet<Long>>> mToCopy = MapFact.mMap(AMap.addMergeMapSets()); // в какое свойство, из какого свойства - какой класс
        for (DBConcreteClass oldClass : oldDBStructure.concreteClasses) {
            for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                if (oldClass.sID.equals(newClass.sID)) {
                    if (!(oldClass.dataPropCN.equals(newClass.dataPropCN))) // надо пометить перенос, и удаление
                        mToCopy.add(newClass.dataPropCN, MapFact.singleton(oldClass.dataPropCN, SetFact.singleton(oldClass.ID)));
                    break;
                }
            }
        }
        return mToCopy.immutable();
    }

    private void createAdditionalIndexRecords(OldDBStructure dbStructure) {
        for (List<IndexData<String>> indexList : dbStructure.tables.values()) {
            List<IndexData<String>> additionalIndexes = new ArrayList<>();
            for (IndexData<String> indexData : indexList) {
                if (indexData.options.type == IndexType.LIKE) {
                    additionalIndexes.add(new IndexData<>(new ArrayList<>(indexData.fields), indexData.options.changeType(IndexType.DEFAULT)));
                } else if (indexData.options.type == IndexType.MATCH) {
                    // We don't need to add indexes if the index was built for TSVECTOR type, but we don't have that information
                    // And it should be fine while we are dropping indexes with IF EXISTS
                    additionalIndexes.add(new IndexData<>(new ArrayList<>(indexData.fields), indexData.options.changeType(IndexType.DEFAULT)));
                    additionalIndexes.add(new IndexData<>(new ArrayList<>(indexData.fields), indexData.options.changeType(IndexType.LIKE)));
                }
            }
            indexList.addAll(additionalIndexes);
        }
    }

    public static void moveObjects(DBTable table, SQLSession sql, OldDBStructure oldDBStructure, ImMap<String, ImSet<Long>> copyFrom, DBStoredProperty classProp, BaseClass baseClass) throws Exception {
//        ImplementTable.ignoreStatProps(() -> {
            QueryBuilder<KeyField, PropertyField> copyObjects = new QueryBuilder<>(table);
            Expr keyExpr = copyObjects.getMapExprs().singleValue();
            Where moveWhere = Where.FALSE();
            CaseExprInterface mExpr = Expr.newCases(true, copyFrom.size());
            MSet<String> mCopyFromTables = SetFact.mSetMax(copyFrom.size());
            for (int j = 0, sizeJ = copyFrom.size(); j < sizeJ; j++) {
                DBStoredProperty oldClassProp = oldDBStructure.getProperty(copyFrom.getKey(j));
                DBTable oldTable = oldDBStructure.getTable(oldClassProp.tableName);
                mCopyFromTables.add(oldClassProp.tableName);

                Expr oldExpr = oldTable.join(MapFact.singleton(oldTable.getTableKeys().single(), keyExpr)).getExpr(oldTable.findProperty(oldClassProp.getDBName()));
                Where moveExprWhere = Where.FALSE();
                for (long prevID : copyFrom.getValue(j))
                    moveExprWhere = moveExprWhere.or(oldExpr.compare(new DataObject(prevID, baseClass.objectClass), Compare.EQUALS));
                mExpr.add(moveExprWhere, oldExpr);
                moveWhere = moveWhere.or(moveExprWhere);
            }
            copyObjects.addProperty(table.findProperty(classProp.getDBName()), mExpr.getFinal());
            copyObjects.and(moveWhere);

            startLog(localize(LocalizedString.createFormatted("{logics.info.objects.transferring.from.tables.to.table}", classProp.tableName, mCopyFromTables.immutable().toString())));
            sql.modifyRecords(new ModifyQuery(table, copyObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
//            return null;
//        });
    }

    private void classForNameSQL() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Class.forName("com.informix.jdbc.IfxDriver");
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    public void writeDroppedColumns(DataSession session, Map<String, String> columnsToDrop) throws SQLException, SQLHandledException {
        for (String sid : columnsToDrop.keySet()) {
            DataObject object = session.addObject(reflectionLM.dropColumn);
            reflectionLM.sidDropColumn.change(sid, session, object);
            reflectionLM.sidTableDropColumn.change(columnsToDrop.get(sid), session, object);
            reflectionLM.timeDropColumn.change(LocalDateTime.now(), session, object);
            reflectionLM.revisionDropColumn.change(getRevision(SystemProperties.inDevMode), session, object);
        }
        apply(session);
    }

    private void setDefaultUserLocalePreferences(DataSession session) throws SQLException, SQLHandledException {
        businessLogics.authenticationLM.defaultLanguage.change(defaultUserLanguage, session);
        businessLogics.authenticationLM.defaultCountry.change(defaultUserCountry, session);
        businessLogics.authenticationLM.defaultTimezone.change(defaultUserTimezone, session);
        businessLogics.authenticationLM.defaultTwoDigitYearStart.change(defaultUserTwoDigitYearStart, session);
        businessLogics.authenticationLM.defaultDateFormat.change(defaultUserDateFormat, session);
        businessLogics.authenticationLM.defaultTimeFormat.change(defaultUserTimeFormat, session);

        Locale locale = Locale.getDefault();
        // these params are set with native
        businessLogics.authenticationLM.serverLanguage.change(locale.getLanguage(), session);
        businessLogics.authenticationLM.serverCountry.change(locale.getCountry(), session);
        businessLogics.authenticationLM.serverTimezone.change(TimeZone.getDefault().getID(), session);
        businessLogics.authenticationLM.serverTwoDigitYearStart.change(businessLogics.tFormats.twoDigitYearStart, session);
        businessLogics.authenticationLM.serverDateFormat.change(businessLogics.tFormats.datePattern, session);
        businessLogics.authenticationLM.serverTimeFormat.change(businessLogics.tFormats.timePattern, session);

        apply(session);
    }

    private void setLogicsParams(DataSession session) throws SQLException, SQLHandledException {
        String logicsCaption = businessLogics.logicsCaption;
        String topModule = businessLogics.topModule;
        String theme = businessLogics.theme;
        String size = businessLogics.size;
        String navbar = businessLogics.navbar;
        LM.logicsCaption.change(isRedundantString(logicsCaption) ? null : logicsCaption, session);
        LM.topModule.change(isRedundantString(topModule) ? null : topModule, session);
        systemEventsLM.serverTheme.change(isRedundantString(theme) ? NullValue.instance : systemEventsLM.theme.getDataObject(theme), session);
        systemEventsLM.serverSize.change(isRedundantString(size) ? NullValue.instance : systemEventsLM.size.getDataObject(size), session);
        systemEventsLM.serverNavbar.change(isRedundantString(navbar) ? NullValue.instance : systemEventsLM.navbar.getDataObject(navbar), session);
        systemEventsLM.serverNavigatorPinMode.change(businessLogics.navigatorPinMode, true, session);
        apply(session);
    }

    private void initSystemUser(DataSession session) throws SQLException, SQLHandledException {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
        query.and(query.getMapExprs().singleValue().isClass(businessLogics.authenticationLM.systemUser));
        ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.EMPTYORDER(), new LimitOffset(1)).keyOrderSet();
        if (rows.size() == 0) {
            systemUser = (Long) session.addObject(businessLogics.authenticationLM.systemUser).object;
            apply(session);
        } else
            systemUser = (Long) rows.single().get("key");

        serverComputer = (long) getComputer(SystemUtils.getLocalHostName(), session, getStack()).object;
    }

    private void updateMaterializationStats(DataSession session, List<Pair<Property, Boolean>> recalculateProperties) throws SQLException, SQLHandledException {
        Map<ImplementTable, List<Pair<Property, Boolean>>> propertiesMap;
        if (Settings.get().isGroupByTables()) {
            propertiesMap = new HashMap<>();
            for (Pair<Property, Boolean> property : recalculateProperties)
                propertiesMap.computeIfAbsent(property.first.mapTable.table, k -> new ArrayList<>()).add(property);

            int count = 1;
            int total = propertiesMap.size();
            for(Map.Entry<ImplementTable, List<Pair<Property, Boolean>>> entry : propertiesMap.entrySet())
                recalculateAndUpdateStat(session, entry.getKey(), entry.getValue(), count++, total);
        }
    }

    private void recalculateAndUpdateStat(DataSession session, ImplementTable table, List<Pair<Property, Boolean>> properties, int count, int total) throws SQLException, SQLHandledException {
        ImSet<Pair<Property, Boolean>> propertySet = SetFact.fromJavaOrderSet(properties).getSet();
        ImMap<PropertyField, String> fields = propertySet.mapKeyValues(property -> property.first.field, property -> property.first.getCanonicalName());
        ImSet<PropertyField> skipRecalculateFields = propertySet.filterFn(property -> property.second).mapSetValues(property -> property.first.field);

        Result<ImMap<String, Pair<Integer, Integer>>> propsStat = new Result<>();
        runWithStartLog((E2Runnable<SQLException, SQLHandledException>) () -> {
            table.recalculateStat(reflectionLM, session, fields, skipRecalculateFields, false);
            propsStat.result = table.recalculateStat(reflectionLM, session, fields, skipRecalculateFields, true);
            apply(session);
        }, String.format("Updating materialization stats %s of %s: %s", count, total, table));
            
        table.updateStat(propsStat.result, fields.keys(), false);
    }

    private void migrateReflectionProperties(DataSession session, OldDBStructure oldDBStructure) {
        migrateReflectionProperties(session, oldDBStructure, false);
        migrateReflectionProperties(session, oldDBStructure, true);
    }
    private void migrateReflectionProperties(DataSession session, OldDBStructure oldDBStructure, boolean actions) {
        MigrationVersion oldVersion = oldDBStructure.migrationVersion;
        Map<String, String> nameChanges = actions ? migrationManager.getActionCNChangesAfter(oldVersion) 
                                                  : migrationManager.getPropertyCNChangesAfter(oldVersion);
        ImportField oldCanonicalNameField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField newCanonicalNameField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);

        ConcreteCustomClass customClass = actions ? reflectionLM.action : reflectionLM.property;
        LP objectByName = actions ? reflectionLM.actionCanonicalName : reflectionLM.propertyCanonicalName;
        LP nameByObject = actions ? reflectionLM.canonicalNameAction : reflectionLM.canonicalNameProperty;
        ImportKey<?> keyProperty = new ImportKey(customClass, objectByName.getMapping(oldCanonicalNameField));

        try {
            List<List<Object>> data = new ArrayList<>();
            for (String oldName : nameChanges.keySet()) {
                data.add(Arrays.asList(oldName, nameChanges.get(oldName)));
            }

            List<ImportProperty<?>> properties = new ArrayList<>();
            properties.add(new ImportProperty(newCanonicalNameField, nameByObject.getMapping(keyProperty)));

            ImportTable table = new ImportTable(asList(oldCanonicalNameField, newCanonicalNameField), data);

            IntegrationService service = new IntegrationService(session, table, Collections.singletonList(keyProperty), properties);
            service.synchronize(false, false);
            apply(session);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static <P extends PropertyInterface> Where getIncorrectWhere(ImplementTable table, PropertyField field, BaseClass baseClass, final ImRevMap<KeyField, KeyExpr> mapKeys, Result<Expr> resultExpr) {
        Expr fieldExpr = baseClass.getInconsistentTable(table).join(mapKeys).getExpr(field);
        resultExpr.set(fieldExpr);
        final Where inTable = fieldExpr.getWhere();
        Where correctClasses = table.getClassWhere(field).getWhere(MapFact.addExcl(mapKeys, field, fieldExpr), true, IsClassType.INCONSISTENT);
        return inTable.and(correctClasses.not());
    }

    public static String checkTableClasses(@ParamMessage ImplementTable table, SQLSession session, boolean runInTransaction, BaseClass baseClass, boolean includeProps) throws SQLException, SQLHandledException {
        Result<String> rResult = new Result<>();
        run(session, runInTransaction, CHECK_CLASSES_TIL, sql -> rResult.set(checkTableClasses(table, sql, null, baseClass, includeProps)));
        return rResult.result;
    }

    public String recalculateMaterializations(ExecutionStack stack, SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        try(DataSession dataSession = createRecalculateSession(session)){
            List<String> messageList = recalculateMaterializations(dataSession, session, businessLogics.getRecalculateAggregateStoredProperties(dataSession, false), isolatedTransaction, serviceLogger);
            apply(dataSession, stack);
            return businessLogics.formatMessageList(messageList);
        }
    }

    public void ensureLogLevel() {
        getSyntax().setLogLevel(Settings.get().getLogLevelJDBC());
        getAdapter().ensureLogLevel();
    }

    public interface RunServiceData {
        void run(SessionCreator sql) throws SQLException, SQLHandledException;
    }

    public static void runData(SessionCreator creator, boolean runInTransaction, RunServiceData run) throws SQLException, SQLHandledException {
        if(runInTransaction) {
            ExecutionContext context = (ExecutionContext) creator;
            try(ExecutionContext.NewSession newContext = context.newSession()) {
                run.run(newContext.getSession());
                newContext.apply();
            }
        } else
            run.run(creator);
    }

    public interface RunService {
        void run(SQLSession sql) throws SQLException, SQLHandledException;
    }

    public static String checkTableClasses(@ParamMessage ImplementTable table, SQLSession sql, QueryEnvironment env, BaseClass baseClass, boolean includeProps) throws SQLException, SQLHandledException {
        Query<KeyField, Object> query = getIncorrectQuery(table, baseClass, includeProps, true);

        String incorrect = env == null ? query.readSelect(sql) : query.readSelect(sql, env);
        if(!incorrect.isEmpty())
            return "---- Checking Classes for table : " + table + "-----" + '\n' + incorrect;
        return "";
    }

    private static Query<KeyField, Object> getIncorrectQuery(ImplementTable table, BaseClass baseClass, boolean includeProps, boolean check) {
        ImRevMap<KeyField, KeyExpr> mapKeys = table.getMapKeys();
        Where where = (includeProps && !check) ? Where.FALSE() : getIncorrectWhere(table, baseClass, mapKeys);
        ImMap<Object, Expr> propExprs;
        if(includeProps) {
            Where keyWhere = where;
            final ImValueMap<PropertyField, Expr> mPropExprs = table.properties.mapItValues();
            for (int i=0,size=table.properties.size();i<size;i++) {
                final PropertyField field = table.properties.get(i);
                Result<Expr> propExpr = new Result<>();
                Where propWhere = getIncorrectWhere(table, field, baseClass, mapKeys, propExpr);
                where = where.or(propWhere);
                mPropExprs.mapValue(i, check ? ValueExpr.TRUE.and(propWhere) : propExpr.result.and(propWhere.not()));
            }
            propExprs = BaseUtils.immutableCast(mPropExprs.immutableValue());
            if(check)
                propExprs = propExprs.addExcl("KEYS", ValueExpr.TRUE.and(keyWhere));
        } else
            propExprs = MapFact.EMPTY();

        return new Query<>(mapKeys, propExprs, where);
    }

    public static void recalculateTableClasses(ImplementTable table, SQLSession sql, boolean runInTransaction, BaseClass baseClass) throws SQLException, SQLHandledException {
        run(sql, runInTransaction, RECALC_CLASSES_TIL, sql1 -> recalculateTableClasses(table, sql1, null, baseClass));
    }

    public List<AggregateProperty> getDependentProperties(DataSession dataSession, Property<?> property, Set<Property> calculated, boolean dependencies) throws SQLException, SQLHandledException {
        List<AggregateProperty> properties = new ArrayList<>();
        if (dependencies) {
            for (Property prop : property.getDepends(false)) {
                if (prop != property && !calculated.contains(prop)) {
                    properties.addAll(getDependentProperties(dataSession, prop, calculated, true));
                }
            }
        }

        if(property instanceof AggregateProperty && property.isStored()) {
            properties.add((AggregateProperty) property);
            calculated.add(property);
        }

        if(!dependencies) {
            for (Property prop : businessLogics.getRecalculateAggregateStoredProperties(dataSession, true)) {
                if (prop != property && !calculated.contains(prop) && Property.depends(prop, property, false)) {
                    boolean recalculate = reflectionLM.disableMaterializationsTableColumn.read(dataSession, reflectionLM.tableColumnSID.readClasses(dataSession, new DataObject(property.getDBName()))) == null;
                    if(recalculate) {
                        properties.addAll(getDependentProperties(dataSession, prop, calculated, false));
                    }
                }
            }
        }
        return properties;
    }

    public List<String> recalculateMaterializations(DataSession dataSession, SQLSession session, final List<AggregateProperty> recalculateProperties, boolean isolatedTransaction, Logger logger) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<>();
        final int total = recalculateProperties.size();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        if(total > 0) {
            for (int i = 0; i < recalculateProperties.size(); i++) {
                recalculateMaterialization(dataSession, session, isolatedTransaction, new ProgressBar(localize("{logics.recalculation.materializations}"), i + 1, total), messageList, maxRecalculateTime, recalculateProperties.get(i), logger);
            }
        }
        return messageList;
    }

    @StackProgress
    private void recalculateMaterialization(final DataSession dataSession, SQLSession session, boolean isolatedTransaction, @StackProgress final ProgressBar progressBar, final List<String> messageList, final long maxRecalculateTime, @ParamMessage final AggregateProperty property, final Logger logger) throws SQLException, SQLHandledException {
        long start = System.currentTimeMillis();
        logger.info(String.format("Recalculating materialization %s of %s started: %s", progressBar.progress, progressBar.total, property.getSID()));
        property.recalculateMaterialization(businessLogics, dataSession, session, isolatedTransaction, LM.baseClass);

        long time = System.currentTimeMillis() - start;
        String message = String.format("Recalculating materialization: %s, %sms", property.getSID(), time);
        logger.info(message);
        if (time > maxRecalculateTime)
            messageList.add(message);
    }

    @StackMessage("{logics.recalculating.data.classes}")
    public static void recalculateTableClasses(ImplementTable table, SQLSession sql, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        Query<KeyField, PropertyField> query;

        query = BaseUtils.immutableCast(getIncorrectQuery(table, baseClass, false, false));
        sql.deleteRecords(new ModifyQuery(table, query, env == null ? OperationOwner.unknown : env.getOpOwner(), TableOwner.global));

        query = BaseUtils.immutableCast(getIncorrectQuery(table, baseClass, true, false));
        if(!query.properties.isEmpty())
            sql.updateRecords(new ModifyQuery(table, query, env == null ? OperationOwner.unknown : env.getOpOwner(), TableOwner.global));
    }

    public static void run(SQLSession session, boolean runInTransaction, boolean serializable, RunService run) throws SQLException, SQLHandledException {
        run(session, runInTransaction, serializable, run, 0);
    }


    public void recalculateMaterializationTableColumn(DataSession dataSession, SQLSession session, String propertyCanonicalName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        AggregateProperty property = businessLogics.getAggregateStoredProperty(propertyCanonicalName);
        if (property != null)
            runMaterializationRecalculation(dataSession, session, property, null, isolatedTransaction);
    }

    public <P extends PropertyInterface> void runMaterializationRecalculation(final DataSession dataSession, SQLSession session, final AggregateProperty<P> aggregateProperty, PropertyChange<P> where, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        runMaterializationRecalculation(dataSession, session, aggregateProperty, where, isolatedTransaction, null);
    }

    public <P extends PropertyInterface> void runMaterializationRecalculation(final DataSession dataSession, SQLSession session, final AggregateProperty<P> aggregateProperty, PropertyChange<P> where, boolean isolatedTransaction, Boolean recalculateClasses) throws SQLException, SQLHandledException {
        aggregateProperty.recalculateMaterialization(businessLogics, dataSession, session, LM.baseClass, where, recalculateClasses, isolatedTransaction);
    }

    public <P extends PropertyInterface> void runRecalculateClasses(SQLSession session, final AbstractDataProperty abstractDataProperty, PropertyChange<ClassPropertyInterface> where, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        abstractDataProperty.recalculateClasses(session, isolatedTransaction, LM.baseClass, where);
    }

    public void recalculateMaterializationWithDependenciesTableColumn(SQLSession session, ExecutionStack stack, String propertyCanonicalName, boolean isolatedTransaction, boolean dependencies) throws SQLException, SQLHandledException {
        try(DataSession dataSession = createRecalculateSession(session)) {
            List<AggregateProperty> properties = getDependentProperties(dataSession, businessLogics.findProperty(propertyCanonicalName).property, new HashSet<>(), dependencies);
            for(AggregateProperty prop : properties) {
                runMaterializationRecalculation(dataSession, session, prop, null, isolatedTransaction);
            }
            apply(dataSession, stack);
        }
    }

    public Set<String> getDisableStatsTableSet(DataSession session) throws SQLException, SQLHandledException {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
        ImSet<String> notRecalculateStatsTableSet = SetFact.EMPTY();
        Expr expr = reflectionLM.disableStatsTableSID.getExpr(query.getMapExprs().singleValue());
        query.and(expr.getWhere());
        return query.execute(session).keys().mapSetValues(value -> (String) value.singleValue()).toJavaSet();
    }

    public Set<String> getDisableClassesTableSet(DataSession session) throws SQLException, SQLHandledException {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
        ImSet<String> disableClassesTableSet = SetFact.EMPTY();
        Expr expr = reflectionLM.disableClassesTableSID.getExpr(query.getMapExprs().singleValue());
        query.and(expr.getWhere());
        return query.execute(session).keys().mapSetValues(value -> (String) value.singleValue()).toJavaSet();
    }

    public Set<String> getDisableStatsTableColumnSet() {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
        ImSet<String> disableStatsTableColumnSet = SetFact.EMPTY();
        try (final DataSession dataSession = createSession()) {
            Expr expr = reflectionLM.disableStatsTableColumnSID.getExpr(query.getMapExprs().singleValue());
            query.and(expr.getWhere());
            disableStatsTableColumnSet = query.execute(dataSession).keys().mapSetValues(value -> (String) value.singleValue());

        } catch (Exception e) {
            serviceLogger.info(e.getMessage());
        }
        return disableStatsTableColumnSet.toJavaSet();
    }


    private void checkModules(OldDBStructure dbStructure) {
        String droppedModules = "";
        for (String moduleName : dbStructure.modulesList)
            if (businessLogics.getSysModule(moduleName) == null) {
                startLog("Module " + moduleName + " has been dropped");
                droppedModules += moduleName + ", ";
            }
        if (isDenyDropModules() && !droppedModules.isEmpty())
            throw new RuntimeException("Dropping modules: " + droppedModules.substring(0, droppedModules.length() - 2) + "\nNow, dropping modules is restricted by settings. If you are sure you want to drop these modules, you can set 'db.denyDropModules = false'\nin settings.properties or through other methods. For more information, please visit: https://docs.lsfusion.org/Launch_parameters/#applsfusion\"");
    }

    private synchronized void runMigrationScript() {
        if (ignoreMigration) {
            //todo: добавить возможность задавать расположение для migration.script, чтобы можно было запускать разные логики из одного модуля
            return;
        }

        if (!migrationScriptWasRead) {
            try {
                migrationManager = new MigrationManager();
                InputStream scriptStream = getClass().getResourceAsStream("/migration.script");
                if (scriptStream != null) {
                    ANTLRInputStream stream = new ANTLRInputStream(scriptStream);
                    MigrationScriptLexer lexer = new MigrationScriptLexer(stream);
                    MigrationScriptParser parser = new MigrationScriptParser(new CommonTokenStream(lexer));

                    parser.self = migrationManager;

                    parser.script();

                    checkMigrationScriptParsingErrors(lexer, parser);

                    migrationScriptWasRead = true;
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void checkMigrationScriptParsingErrors(MigrationScriptLexer lexer, MigrationScriptParser parser) {
        if (!lexer.getErrors().isEmpty() || !parser.getErrors().isEmpty()) {
            String errorsText = Stream.concat(lexer.getErrors().stream(), parser.getErrors().stream()).collect(Collectors.joining("\n"));
            throw new RuntimeException("Migration script parsing completed with the following errors:\n" + errorsText);
        }
    }

    private void renameColumn(SQLSession sql, OldDBStructure oldData, DBStoredProperty oldProperty, String newName) throws SQLException {
        String oldName = oldProperty.getDBName();
        if (!oldName.equalsIgnoreCase(newName)) {
            startLog("Renaming column from " + oldName + " to " + newName + " in table " + oldProperty.tableName);
            sql.renameColumn(oldProperty.getTableName(getSyntax()), oldName, newName);
        }
        PropertyField field = oldData.getTable(oldProperty.tableName).findProperty(oldName);
        field.setName(newName);
    }
    
    private void renameMigratingProperties(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        Map<String, String> propertyCNChanges = migrationManager.getStoredPropertyCNChangesAfter(oldData.migrationVersion);
        Map<String, DBStoredProperty> propByCN = storedPropertiesMap(newData.storedProperties);
        Set<String> renamedCN = new HashSet<>();

        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            String canonicalName = oldProperty.getCanonicalName();
            if (propertyCNChanges.containsKey(canonicalName)) {
                canonicalName = propertyCNChanges.get(canonicalName);
            }
            DBStoredProperty newProperty = propByCN.get(canonicalName);
            if (newProperty != null) {
                renamedCN.add(oldProperty.getCanonicalName());
                renameColumn(sql, oldData, oldProperty, newProperty.getDBName());
                oldProperty.migrateNames(newProperty.getCanonicalName(), newProperty.getDBName());
            }
        }

        propertyCNChanges.forEach((oldCN, newCN) -> {
            if (!renamedCN.contains(oldCN)) {
                startLogWarn("Property " + oldCN + " was not found for renaming to " + newCN);
            }
        });
    }

    private Map<String, DBStoredProperty> storedPropertiesMap(List<DBStoredProperty> properties) {
        return properties.stream().collect(Collectors.toMap(DBStoredProperty::getCanonicalName, property -> property));
    }

    private void renameMigratingTables(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        Map<String, String> tableRenames = getTableRenames(oldData, newData);
        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            if (tableRenames.containsKey(oldProperty.tableName)) {
                oldProperty.tableName = tableRenames.get(oldProperty.tableName);
            }
        }

        for (DBTable table : oldData.tables.keySet()) {
            String tableDBName = table.getName();
            if (tableRenames.containsKey(tableDBName)) {
                String newDBName = tableRenames.get(tableDBName);
                if (!tableDBName.equalsIgnoreCase(newDBName)) {
                    startLog("Renaming table from " + tableDBName + " to " + newDBName);
                    sql.renameTable(table, newDBName);
                }
                table.setName(newDBName);
            }
        }
    }

    private Map<String, String> getTableRenames(OldDBStructure oldData, NewDBStructure newData) {
        Map<String, String> tableCNChanges = migrationManager.getTableCNChangesAfter(oldData.migrationVersion);
        if (oldData.version < 38) {
            return getTableSIDChanges(tableCNChanges);
        }

        Map<String, String> tableRenames = new HashMap<>();
        Map<String, DBTable> newTablesMap = createNewTablesMap(newData);
        for (DBTable table : oldData.tables.keySet()) {
            String oldCN = table.getCanonicalName();
            if (oldCN != null) {
                String newCN = tableCNChanges.getOrDefault(oldCN, oldCN);
                if (newTablesMap.containsKey(newCN)) {
                    String oldDBName = table.getName();
                    String newDBName = newTablesMap.get(newCN).getName();
                    if (!oldDBName.equals(newDBName)) {
                        tableRenames.put(oldDBName, newDBName);
                    }
                }
            }
        }
        return tableRenames;
    }

    private Map<String, DBTable> createNewTablesMap(NewDBStructure newData) {
        return newData.tables.keySet().stream()
                .filter(table -> table.getCanonicalName() != null)
                .collect(Collectors.toMap(DBTable::getCanonicalName, table -> table));
    }

    private Map<String, String> getTableSIDChanges(Map<String, String> tableCNChanges) {
        return tableCNChanges.entrySet().stream()
                    .collect(Collectors.toMap(
                        entry -> getNamingPolicy().transformTableCNToDBName(entry.getKey()),
                        entry -> getNamingPolicy().transformTableCNToDBName(entry.getValue()))
                    );
    }

    private void renameMigratingClasses(OldDBStructure oldData) {
        Map<String, String> classChanges = migrationManager.getClassSIDChangesAfter(oldData.migrationVersion);
        for (DBConcreteClass oldClass : oldData.concreteClasses) {
            if(classChanges.containsKey(oldClass.sID)) {
                oldClass.sID = classChanges.get(oldClass.sID);
            }
        }
    }
    
    private void migrateClassProperties(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        // Изменим в старой структуре классовые свойства. Предполагаем, что в одной таблице может быть только одно классовое свойство. Переименовываем поля в таблицах
        Map<String, String> tableNewClassProps = new HashMap<>();
        for (DBConcreteClass cls : newData.concreteClasses) {
            DBStoredProperty classProp = newData.getProperty(cls.dataPropCN);
            assert classProp != null;
            String tableName = classProp.getTable().getName();
            if (tableNewClassProps.containsKey(tableName)) {
                assert cls.dataPropCN.equals(tableNewClassProps.get(tableName));
            } else {
                tableNewClassProps.put(tableName, cls.dataPropCN);
            }
        }
        
        Map<String, String> nameRenames = new HashMap<>();
        for (DBConcreteClass cls : oldData.concreteClasses) {
            if (!nameRenames.containsKey(cls.dataPropCN)) {
                DBStoredProperty oldClassProp = oldData.getProperty(cls.dataPropCN);
                assert oldClassProp != null;
                String tableName = oldClassProp.tableName;
                if (tableNewClassProps.containsKey(tableName)) {
                    String newName = tableNewClassProps.get(tableName);
                    nameRenames.put(cls.dataPropCN, newName);
                    String newDBName = getNamingPolicy().transformActionOrPropertyCNToDBName(newName);
                    renameColumn(sql, oldData, oldClassProp, newDBName);
                    oldClassProp.migrateNames(newName);
                    cls.dataPropCN = newName;
                }
            } else {
                cls.dataPropCN = nameRenames.get(cls.dataPropCN);
            }
        }
    }


    // Временная реализация для переименования
    // issue #4672 Построение сигнатуры LOG-свойств
    // Проблема текущего способа: 
    // Каноническое имя log-свойств сейчас определяется не из сигнатуры базового свойства, а из getInterfaces, которые могут не совпадать с сигнатурой
    // В дальнейшем. когда сделаем нормальные канонические имена у log-свойств, можно перенести этот функционал в setUserLoggableProperties 
    // и добавлять изменения канонических имен log-свойств напрямую в storedPropertyCNChanges еще на том шаге
    private void addLogPropertiesToMigration(OldDBStructure oldData, MigrationVersion newMigrationVersion) {
        Map<String, String> changes = migrationManager.getPropertyCNChangesAfter(oldData.migrationVersion);
        Map<String, String> rChanges = BaseUtils.reverse(changes);
        
        Set<String> logProperties = new HashSet<>();
        
        for (LP<?> lp : businessLogics.getNamedProperties()) {
            if (lp.property.getName().startsWith(PropertyCanonicalNameUtils.logPropPrefix)) {
                logProperties.add(lp.property.getCanonicalName());
            }
        }
        
        for (LP<?> lp : businessLogics.getNamedProperties()) {
            if (lp.property.isFull(AlgType.logType)) {
                String logPropCN = LogicsModule.getLogPropertyCN(lp, "System", businessLogics.systemEventsLM);
                if (logProperties.contains(logPropCN)) {
                    String propCN = lp.property.getCanonicalName();
                    if (rChanges.containsKey(propCN)) {
                        String oldPropCN = rChanges.get(propCN);
                        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(businessLogics, oldPropCN);
                        try {
                            String oldLogPropCN = LogicsModule.getLogPropertyCN("System", parser.getNamespace(), parser.getName(),
                                    LogicsModule.getSignatureForLogProperty(parser.getSignature(), businessLogics.systemEventsLM));

                            migrationManager.addStoredPropertyCNChange(newMigrationVersion.toString(), oldLogPropCN, logPropCN);
                        } catch (CanonicalNameUtils.ParseException e) {
                            startLog(String.format("Cannot migrate LOG property to '%s': '%s'", logPropCN, e.getMessage()));
                        }
                    }
                }
            }
        }
    }
    
    // Не разбирается с индексами. Было решено, что сохранять индексы необязательно.
    private void alterDBStructure(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        startLog("Applying migration script (" + oldData.migrationVersion + " -> " + newData.migrationVersion + ")");
        // Сохраним изменения имен свойств на форме и элементов навигатора для reflectionManager
        finalPropertyDrawNameChanges = migrationManager.getPropertyDrawNameChangesAfter(oldData.migrationVersion);
        finalNavigatorElementNameChanges = migrationManager.getNavigatorCNChangesAfter(oldData.migrationVersion);

        // Обязательно до renameMigratingProperties, потому что в storedPropertyCNChanges добавляются изменения для log-свойств 
        addLogPropertiesToMigration(oldData, newData.migrationVersion);

        // Переименовываем поля в таблицах при изменении dbName (в том числе из-за изменения naming policy) и/или миграции
        // При этом в старой структуре тоже все переименовываем: имя поля, каноническое имя свойства и dbName свойства
        renameMigratingProperties(sql, oldData, newData);
        
        // Переименовываем таблицы из скрипта миграции, переустанавливаем ссылки на таблицы в свойствах
        renameMigratingTables(sql, oldData, newData);

        // Переустановим имена классовым свойствам, если это необходимо. Также при необходимости переименуем поля в таблицах   
        // Имена полей могут измениться при переименовании таблиц (так как в именах классовых свойств есть имя таблицы) либо при изменении dbNamePolicy
        migrateClassProperties(sql, oldData, newData);        

        // переименовываем классы из скрипта миграции
        renameMigratingClasses(oldData);
    }

    public Map<String, String> getPropertyDrawNamesChanges() {
        return finalPropertyDrawNameChanges;
    } 
    
    public Map<String, String> getNavigatorElementNameChanges() {
        return finalNavigatorElementNameChanges;
    }

    @NFLazy
    public <Z extends PropertyInterface> void addIndex(ImList<PropertyObjectInterfaceImplement<String>> index, String dbName, IndexType indexType) {
        PropertyRevImplement<Z, String> propertyImplement = (PropertyRevImplement<Z, String>) findProperty(index);
        if(propertyImplement != null) {
            indexes.put(index, new IndexOptions(propertyImplement.property.getType() instanceof DataClass, indexType, dbName));
            propertyImplement.property.markIndexed(propertyImplement.mapping, index, indexType);
        }
    }

    private static PropertyRevImplement<?, String> findProperty(ImList<PropertyObjectInterfaceImplement<String>> index) {
        for (PropertyObjectInterfaceImplement<String> lp : index) {
            if(lp instanceof PropertyRevImplement) {
                return (PropertyRevImplement<?, String>) lp;
            }
        }
        return null;
    }

    public boolean checkBackupParams(ExecutionContext context) {
        return adapter.checkBackupParams(context);
    }

    public String getBackupFilePath(String dumpFileName) {
        return adapter.getBackupFilePath(dumpFileName);
    }

    public String getBackupFileLogPath(String dumpFileName) {
        return adapter.getBackupFileLogPath(dumpFileName);
    }

    public void backupDB(ExecutionContext context, String dumpFileName, int threadCount, List<String> excludeTables) throws IOException, InterruptedException {
        adapter.backupDB(context, dumpFileName, threadCount, excludeTables);
    }

    public String customRestoreDB(String fileBackup, Set<String> tables, boolean isMultithread) throws IOException {
        return adapter.customRestoreDB(fileBackup, tables, isMultithread);
    }

    public void dropDB(String dbName) throws IOException {
        adapter.dropDB(dbName);
    }

    public List<List<List<Object>>> readCustomRestoredColumns(String dbName, String table, List<String> keys, List<String> columns) throws SQLException {
        return adapter.readCustomRestoredColumns(dbName, table, keys, columns);
    }

    public void analyzeDB(SQLSession session) throws SQLException {
        session.executeDDL(getSyntax().getAnalyze());
    }

    public void vacuumDB(SQLSession session) throws SQLException {
        session.executeDDL(getSyntax().getVacuumDB());
    }

    private static void run(SQLSession session, boolean runInTransaction, boolean serializable, RunService run, int attempts) throws SQLException, SQLHandledException {
        if(runInTransaction) {
            session.startTransaction(serializable, OperationOwner.unknown);
            try {
                run.run(session);
                session.commitTransaction();
            } catch (Throwable t) {
                session.rollbackTransaction();
                if(t instanceof SQLHandledException && ((SQLHandledException)t).repeatApply(session, OperationOwner.unknown, attempts)) { // update conflict или deadlock или timeout - пробуем еще раз
                    //serviceLogger.error("Run error: ", t);
                    run(session, true, serializable, run, attempts + 1);
                    return;
                }

                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            }
        } else
            run.run(session);
    }

    public static void packTables(SQLSession session, ImCol<ImplementTable> tables, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        startLog("Packing tables");
        for (final ImplementTable table : tables) {
            logger.debug(localize("{logics.info.packing.table}") + " (" + table + ")... ");
            packTable(session, table, isolatedTransaction);
            logger.debug("Done");
        }
    }

    public static void packTable(SQLSession session, ImplementTable table, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, PACK_TIL, sql -> sql.packTable(table, OperationOwner.unknown, TableOwner.global));
    }

    public String checkClasses(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        String message = checkClasses(session, isolatedTransaction, LM.baseClass);
        for(ImplementTable implementTable : LM.tableFactory.getImplementTables()) {
            message += checkTableClasses(implementTable, session, isolatedTransaction, LM.baseClass, false); // так как снизу есть проверка классов
        }
        ImOrderSet<AbstractDataProperty> storedDataProperties;
        try(DataSession dataSession = createRecalculateSession(session)) {
            storedDataProperties = businessLogics.getStoredDataProperties(dataSession);
        }
        for (Property property : storedDataProperties)
            message += property.checkClasses(session, isolatedTransaction, LM.baseClass);
        return message;
    }

    public void recalculateClassesExclusiveness(final SQLSession sql, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        runClassesExclusiveness(sql, isolatedTransactions, query -> {
            SingleKeyTableUsage<String> table = new SingleKeyTableUsage<>("recexcl", ObjectType.instance, SetFact.toOrderExclSet("sum", "agg"), key -> key.equals("sum") ? ValueExpr.COUNTCLASS : StringClass.getv(false, ExtInt.UNLIMITED));

            table.writeRows(sql, query, LM.baseClass, DataSession.emptyEnv(OperationOwner.unknown), SessionTable.nonead);

            MExclMap<ConcreteCustomClass, MExclSet<String>> mRemoveClasses = MapFact.mExclMap();
            for (Object distinct : table.readDistinct("agg", sql, OperationOwner.unknown)) { // разновидности agg читаем
                String classes = (String) distinct;
                ConcreteCustomClass keepClass = null;
                for (String singleClass : classes.split(",")) {
                    ConcreteCustomClass customClass = LM.baseClass.findConcreteClassID(Long.parseLong(singleClass));
                    if (customClass != null) {
                        if (keepClass == null)
                            keepClass = customClass;
                        else {
                            ConcreteCustomClass removeClass;
                            if (keepClass.isChild(customClass)) {
                                removeClass = keepClass;
                                keepClass = customClass;
                            } else
                                removeClass = customClass;

                            MExclSet<String> mRemoveStrings = mRemoveClasses.get(removeClass);
                            if (mRemoveStrings == null) {
                                mRemoveStrings = SetFact.mExclSet();
                                mRemoveClasses.exclAdd(removeClass, mRemoveStrings);
                            }
                            mRemoveStrings.exclAdd(classes);
                        }
                    }
                }
            }
            ImMap<ConcreteCustomClass, ImSet<String>> removeClasses = MapFact.immutable(mRemoveClasses);

            for (int i = 0, size = removeClasses.size(); i < size; i++) {
                KeyExpr key = new KeyExpr("key");
                Expr aggExpr = table.join(key).getExpr("agg");
                Where where = Where.FALSE();
                for (String removeString : removeClasses.getValue(i))
                    where = where.or(aggExpr.compare(new DataObject(removeString, StringClass.text), Compare.EQUALS));
                removeClasses.getKey(i).dataProperty.dropInconsistentClasses(sql, LM.baseClass, key, where, OperationOwner.unknown);
            }
        }, LM.baseClass);
    }

    public void uploadToDB(SQLSession sql, boolean isolatedTransactions, final DataAdapter adapter) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, SQLHandledException {
        final OperationOwner owner = OperationOwner.unknown;
        final SQLSession sqlFrom = new SQLSession(adapter, contextProvider);

        sql.pushNoQueryLimit();
        try {
            ImSet<DBTable> tables = SetFact.addExcl(LM.tableFactory.getImplementTables(), IDTable.instance);
            final int size = tables.size();
            for (int i = 0; i < size; i++) {
                final DBTable implementTable = tables.get(i);
                final int fi = i;
                run(sql, isolatedTransactions, UPLOAD_TIL, sql1 -> uploadTableToDB(sql1, implementTable, fi + "/" + size, sqlFrom, owner));
            }
        } finally {
            sql.popNoQueryLimit();
        }
    }

    public String checkMaterializations(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        List<AggregateProperty> checkProperties;
        try(DataSession dataSession = createRecalculateSession(session)) {
            checkProperties = businessLogics.getRecalculateAggregateStoredProperties(dataSession, false);
        }
        String message = "";
        for (int i = 0; i < checkProperties.size(); i++) {
            Property property = checkProperties.get(i);
            if(property != null)
                message += ((AggregateProperty) property).checkMaterialization(session, LM.baseClass, new ProgressBar(localize("{logics.info.checking.materialized.property}"), i, checkProperties.size(), property.toString()), isolatedTransaction);
        }
        return message;
    }

//    public String checkStats(SQLSession session) throws SQLException, SQLHandledException {
//        ImOrderSet<Property> checkProperties = businessLogics.getPropertyList();
//
//        double cnt = 0;
//        List<Double> sum = new ArrayList<Double>();
//        List<List<Double>> sumd = new ArrayList<List<Double>>();
//        for(int i=0;i<4;i++) {
//            sum.add(0.0);
//            sumd.add(new ArrayList<Double>());
//        }
//
//        final Result<Integer> proceeded = new Result<Integer>(0);
//        int total = checkProperties.size();
//        ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
//        try {
//            String message = "";
//            for (Property property : checkProperties) {
//                if(property instanceof AggregateProperty) {
//                    List<Double> diff = ((AggregateProperty) property).checkStats(session, LM.baseClass);
//                    if(diff != null) {
//                        for(int i=0;i<4;i++) {
//                            sum.set(i, sum.get(i) + diff.get(i));
//                            sumd.get(i).add(diff.get(i));
//                            cnt++;
//                        }
//                    }
//                }
//
//                if(cnt % 100 == 0) {
//                    for(int i=0;i<4;i++) {
//                        double avg = (double) sum.get(i) / (double) cnt;
//                        double disp = 0;
//                        for (double diff : sumd.get(i)) {
//                            disp += ((double) diff - avg) * ((double) diff - avg);
//                        }
//                        System.out.println("I: " + i + "AVG : " + avg + " DISP : " + (disp) / cnt);
//                    }
//                }
//
//                proceeded.set(proceeded.result + 1);
//                ThreadLocalContext.popActionMessage();
//                ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
//            }
//            return message;
//        } finally {
//            ThreadLocalContext.popActionMessage();
//        }
//    }
//
    public String checkMaterializationTableColumn(SQLSession session, String propertyCanonicalName, boolean runInTransaction) throws SQLException, SQLHandledException {
        Property property = businessLogics.getAggregateStoredProperty(propertyCanonicalName);
        return property != null ? ((AggregateProperty) property).checkMaterialization(session, LM.baseClass, runInTransaction) : null;
    }

    public String recalculateClasses(SQLSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        recalculateClassesExclusiveness(session, isolatedTransactions);

        final List<String> messageList = new ArrayList<>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        for (final ImplementTable implementTable : LM.tableFactory.getImplementTables()) {
            long start = System.currentTimeMillis();
            recalculateTableClasses(implementTable, session, isolatedTransactions, LM.baseClass);
            long time = System.currentTimeMillis() - start;
            String message = String.format("Recalculate Table Classes: %s, %sms", implementTable.toString(), time);
            BaseUtils.serviceLogger.info(message);
            if (time > maxRecalculateTime)
                messageList.add(message);
        }

        try(DataSession dataSession = createRecalculateSession(session)) {
            for (final AbstractDataProperty property : businessLogics.getStoredDataProperties(dataSession)) {
                long start = System.currentTimeMillis();
                property.recalculateClasses(session, isolatedTransactions, LM.baseClass);
                long time = System.currentTimeMillis() - start;
                String message = String.format("Recalculate Class: %s, %sms", property.getSID(), time);
                BaseUtils.serviceLogger.info(message);
                if (time > maxRecalculateTime) messageList.add(message);
            }
            return businessLogics.formatMessageList(messageList);
        }
    }

    public void recalculateTableClasses(SQLSession session, String tableName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (ImplementTable table : LM.tableFactory.getImplementTables())
            if (tableName.equals(table.getName())) {
                recalculateTableClasses(table, session, isolatedTransaction, LM.baseClass);
            }
    }

    public String checkTableClasses(SQLSession session, String tableName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (ImplementTable table : LM.tableFactory.getImplementTables())
            if (tableName.equals(table.getName())) {
                return checkTableClasses(table, session, isolatedTransaction, LM.baseClass, true);
            }
        return null;
    }

    public boolean serializable = true;

    public static String HOSTNAME_COMPUTER;

    public static boolean RECALC_REUPDATE = false;
    public static boolean PROPERTY_REUPDATE = false;

    public void dropColumn(String tableName, String columnName) throws SQLException, SQLHandledException {
        SQLSession sql = getThreadLocalSql();
        sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);
        try {
            sql.dropColumn(tableName, columnName, Settings.get().isStartServerAnyWay());
            packTable(sql, tableName);
            sql.commitTransaction();
        } catch(SQLException e) {
            sql.rollbackTransaction();
            throw e;
        }
    }

    public void dropColumns(String tableName, List<String> columnNames) throws SQLException, SQLHandledException {
        SQLSession sql = getThreadLocalSql();
        sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);
        try {
            sql.dropColumns(tableName, columnNames);
            packTable(sql, tableName);
            sql.commitTransaction();
        } catch(SQLException e) {
            sql.rollbackTransaction();
            throw e;
        }
    }

    private void packTable(SQLSession sql, String tableName) throws SQLException {
        ImplementTable table = LM.tableFactory.getImplementTablesMap().get(tableName); // need to pack table after column is deleted
        if (table != null)
            sql.packTable(table, OperationOwner.unknown, TableOwner.global);
    }

    // может вызываться до инициализации DBManager
    private MigrationVersion getOldMigrationVersion(SQLSession sql) throws IOException, SQLException, SQLHandledException {
        MigrationVersion migrationVersion = new MigrationVersion("0.0");
        
        DataInputStream inputDB;
        StructTable structTable = StructTable.instance;
        RawFileData struct = (RawFileData) sql.readRecord(structTable, MapFact.EMPTY(), structTable.struct, OperationOwner.unknown);
        if (struct != null) {
            inputDB = new DataInputStream(struct.getInputStream());
            readOldDBVersion(inputDB);
            migrationVersion = new MigrationVersion(inputDB.readUTF());
        }
        return migrationVersion;
    }

    public int readOldDBVersion(DataInputStream input) throws IOException {
        int version = input.read() - 'v'; // for backward compatibility
        if (version == 0) {
            version = input.readInt();
        }
        return version;
    }

    public Map<String, String> getPropertyCNChanges(SQLSession sql) {
        runMigrationScript();
        try {
            return migrationManager.getPropertyCNChangesAfter(getOldMigrationVersion(sql));
        } catch (IOException | SQLException | SQLHandledException e) {
            Throwables.propagate(e);
        }
        return new HashMap<>();
    }
    
    private class DBStoredProperty {
        private String dbName;
        private String canonicalName;

        public Boolean isDataProperty;
        public String tableName;
        
        public String getTableName(SQLSyntax syntax) {
            return syntax.getTableName(tableName);
        }
        
        public ImMap<Integer, KeyField> mapKeys;
        public Property<?> property = null;
        public ImplementTable getTable() {
            return property.mapTable.table;
        }

        @Override
        public String toString() {
            return getDBName() + ' ' + tableName;
        }

        public DBStoredProperty(Property<?> property) {
            assert property.isNamed();
            this.canonicalName = property.getCanonicalName();
            this.dbName = property.getDBName();
            this.isDataProperty = property instanceof DataProperty;
            this.tableName = property.mapTable.table.getName();
            this.mapKeys = property.mapTable.mapKeys.mapKeys(value -> value.ID);
            this.property = property;
        }

        public DBStoredProperty(String canonicalName, String dbName, Boolean isDataProperty, String tableName, ImMap<Integer, KeyField> mapKeys) {
            this.canonicalName = canonicalName;
            this.dbName = dbName;
            this.isDataProperty = isDataProperty;
            this.tableName = tableName;
            this.mapKeys = mapKeys;
        }

        public String getDBName() {
            return dbName;
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public void migrateNames(String canonicalName) {
            this.canonicalName = canonicalName;
            this.dbName = getNamingPolicy().transformActionOrPropertyCNToDBName(canonicalName);
        }

        public void migrateNames(String canonicalName, String dbName) {
            this.canonicalName = canonicalName;
            this.dbName = dbName;
        }
    }

    private class DBConcreteClass {
        public String sID;
        public String dataPropCN; // в каком ClassDataProperty хранился

        @Override
        public String toString() {
            return sID + ' ' + dataPropCN;
        }

        public Long ID = null; // только для старых
        public ConcreteCustomClass customClass = null; // только для новых

        private DBConcreteClass(String sID, String dataPropCN, Long ID) {
            this.sID = sID;
            this.dataPropCN = dataPropCN;
            this.ID = ID;
        }

        private DBConcreteClass(ConcreteCustomClass customClass) {
            sID = customClass.getSID();
            dataPropCN = customClass.dataProperty.getCanonicalName();

            this.customClass = customClass;
        }
    }

    private abstract class DBStructure<F> {
        public int version;
        public MigrationVersion migrationVersion;
        public List<String> modulesList = new ArrayList<>();
        public Map<DBTable, List<IndexData<F>>> tables = new HashMap<>(); // actually it's only ImplementTable or SerializedTable
        public List<DBStoredProperty> storedProperties = new ArrayList<>();
        public Set<DBConcreteClass> concreteClasses = new HashSet<>();

        public void writeConcreteClasses(DataOutputStream outDB) throws IOException { // отдельно от write, так как ID заполняются после fillIDs
            outDB.writeInt(concreteClasses.size());
            for (DBConcreteClass concreteClass : concreteClasses) {
                outDB.writeUTF(concreteClass.sID);
                outDB.writeUTF(concreteClass.dataPropCN);
                outDB.writeLong(concreteClass.ID);
            }
        }

        public DBTable getTable(String name) {
            for (DBTable table : tables.keySet()) {
                if (table.getName().equals(name)) {
                    return table;
                }
            }
            return null;
        }

        public DBStoredProperty getProperty(String canonicalName) {
            for (DBStoredProperty prop : storedProperties) {
                if (prop.getCanonicalName().equals(canonicalName)) {
                    return prop;
                }
            }
            return null;
        }
    }

    public static class IndexData<F> {
        public List<F> fields;
        public IndexOptions options;

        public IndexData(List<F> fields, IndexOptions options) {
            this.fields = fields;
            this.options = options;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", fields, options.type);
        }
    }

    // Each index declaration can create more than one physical index. Returns a separate record for each such index
    public <P extends PropertyInterface<?>> Map<ImplementTable, List<IndexData<Field>>> getIndexesMap() {
        Map<ImplementTable, List<IndexData<Field>>> res = new HashMap<>();
        for (ImplementTable table : LM.tableFactory.getImplementTables()) {
            res.put(table, new ArrayList<>());
        }

        for (Map.Entry<ImList<PropertyObjectInterfaceImplement<String>>, IndexOptions> index : indexes.entrySet()) {
            ImList<PropertyObjectInterfaceImplement<String>> indexFields = index.getKey();

            if (indexFields.isEmpty()) {
                throw new RuntimeException(localize("{logics.policy.forbidden.to.create.empty.indexes}"));
            }

            PropertyRevImplement<P, String> basePropertyImplement = (PropertyRevImplement<P, String>) findProperty(indexFields);
            assert basePropertyImplement != null; // исходя из логики addIndex

            Property<P> baseProperty = basePropertyImplement.property;

            if (!baseProperty.isStored())
                throw new RuntimeException(localize("{logics.policy.forbidden.to.create.indexes.on.non.regular.properties}") + " (" + baseProperty + ")");

            ImplementTable baseIndexTable = baseProperty.mapTable.table;
            ImRevMap<String, KeyField> baseMapKeys = basePropertyImplement.mapping.crossJoin(baseProperty.mapTable.mapKeys);

            List<Field> tableIndexFields = new ArrayList<>();

            for (PropertyObjectInterfaceImplement<String> indexField : indexFields) {
                Field field;
                if(indexField instanceof PropertyRevImplement) {
                    PropertyRevImplement<P, String> propertyImplement = (PropertyRevImplement<P, String>)indexField;
                    Property<P> property = propertyImplement.property;

                    if (!property.isStored())
                        throw new RuntimeException(localize("{logics.policy.forbidden.to.create.indexes.on.non.regular.properties}") + " (" + property + ")");

                    ImplementTable indexTable = property.mapTable.table;
                    ImRevMap<String, KeyField> mapKeys = propertyImplement.mapping.crossJoin(property.mapTable.mapKeys);

                    if (!BaseUtils.hashEquals(baseIndexTable, indexTable))
                        throw new RuntimeException(localize(LocalizedString.createFormatted("{logics.policy.forbidden.to.create.indexes.on.properties.in.different.tables}", baseProperty, property)));
                    if (!BaseUtils.hashEquals(baseMapKeys, mapKeys))
                        throw new RuntimeException(localize(LocalizedString.createFormatted("{logics.policy.forbidden.to.create.indexes.on.properties.with.different.mappings}", baseProperty, property, baseMapKeys, mapKeys)));
                    field = property.field;
                } else {
                    field = baseMapKeys.get(((PropertyObjectImplement<String>)indexField).object);
                }
                tableIndexFields.add(field);
            }

            IndexOptions options = index.getValue();
            res.get(baseIndexTable).addAll(getAllCreatedIndexes(tableIndexFields, options));
        }
        return res;
    }

    // We create additional indexes when creating LIKE/MATCH indexes
    private List<IndexData<Field>> getAllCreatedIndexes(List<Field> fields, IndexOptions options) {
        List<IndexData<Field>> res = new ArrayList<>();
        res.add(new IndexData<>(fields, options));

        if (options.type == IndexType.MATCH) {
            if (fields.size() == 1 && fields.get(0).type instanceof TSVectorClass) {
                return res;
            }
            res.add(new IndexData<>(fields, options.changeType(IndexType.LIKE)));
        }
        if (options.type == IndexType.LIKE || options.type == IndexType.MATCH) {
            res.add(new IndexData<>(fields, options.changeType(IndexType.DEFAULT)));
        }
        return res;
    }

    private class NewDBStructure extends DBStructure<Field> {
        
        public NewDBStructure(MigrationVersion migrationVersion, SQLSession sql) {
            version = newDBStructureVersion;
            this.migrationVersion = migrationVersion;

            checkTableDBNameUniqueness();
            tables.putAll(getIndexesMap());

            checkIndexNameUniqueness(sql);
            checkTablesAndIndexesNameUniqueness(sql);

            for (Property<?> property : businessLogics.getStoredProperties()) {
                storedProperties.add(new DBStoredProperty(property));
                assert property.isNamed();
            }

            for (ConcreteCustomClass customClass : businessLogics.getConcreteCustomClasses()) {
                concreteClasses.add(new DBConcreteClass(customClass));
            }
        }

        public class DuplicateRelationNameException extends RuntimeException {
            DuplicateRelationNameException(String message) {
                super(message);
            }
        }

        private void checkTableDBNameUniqueness() {
            final String possibleReasonStr = "This may be caused by the length limitation on database table names, " +
                    "where two different table names are truncated to the same name.";

            Map<String, ImplementTable> tables = new HashMap<>();
            for (ImplementTable table : LM.tableFactory.getImplementTables()) {
                if (tables.containsKey(table.getName())) {
                    String resolveStr, tablesStr;
                    if (table.getCanonicalName() != null) {
                        resolveStr = "In this case, you can either change the table database name or change the naming policy (refer to the documentation for details)";
                        tablesStr = String.format("Tables '%s' and '%s' have the same database name '%s'.",
                                table.getCanonicalName(),
                                tables.get(table.getName()).getCanonicalName(),
                                table.getName());
                    } else {
                        resolveStr = "In this case, you can either explicitly declare the tables or change the naming policy (refer to the documentation for details)";
                        tablesStr = String.format("Auto tables with classes\n\t'%s' and\n\t'%s'\nhave the same database name: '%s'.",
                                table.getMapFields(),
                                tables.get(table.getName()).getMapFields(),
                                table.getName());
                    }
                    throw new DuplicateRelationNameException(String.join("\n","", tablesStr, possibleReasonStr, resolveStr));
                }
                tables.put(table.getName(), table);
            }
        }

        private void checkIndexNameUniqueness(SQLSession sql) {
            Map<String, Pair<DBTable, IndexData<Field>>> indexNames = new HashMap<>();
            for (Map.Entry<DBTable, List<IndexData<Field>>> tableInfo : tables.entrySet()) {
                DBTable table = tableInfo.getKey();
                for (IndexData<Field> index : tableInfo.getValue()) {
                    String name = namingPolicy.transformIndexNameToDBName(sql.getIndexName(table, index));
                    if (indexNames.containsKey(name)) {
                        final String formatStr = "Two indexes has the same database name: '%s'. " +
                                "These indexes are:\n%s in table '%s'\n%s in table '%s'\nOne of the indexes will not be created.";
                        final String msg = String.format(formatStr, name, index, table.getName(), indexNames.get(name).second, indexNames.get(name).first.getName());
                        if (Settings.get().isStartServerAnyWay()) {
                            startLogWarn(msg);
                        } else {
                            throw new DuplicateRelationNameException(msg);
                        }
                    }
                    indexNames.put(name, new Pair<>(table, index));
                }
            }
        }

        private void checkTablesAndIndexesNameUniqueness(SQLSession sql) {
            Map<String, DBTable> tableNames = new HashMap<>();
            for (DBTable table : tables.keySet()) {
                tableNames.put(table.getName(), table);
            }

            for (Map.Entry<DBTable, List<IndexData<Field>>> tableInfo : tables.entrySet()) {
                DBTable indexTable = tableInfo.getKey();
                for (IndexData<Field> index : tableInfo.getValue()) {
                    String indexName = namingPolicy.transformIndexNameToDBName(sql.getIndexName(indexTable, index));

                    if (tableNames.containsKey(indexName)) {
                        DBTable table = tableNames.get(indexName);
                        String reason = "Tables and indexes must have unique names. If a table and an index have identical names, the database cannot distinguish between them, leading to conflicts when executing queries";
                        String formatStr = "\nTable '%s' and index %s have the same database name: '%s'.\n%s";
                        String message = String.format(formatStr, nvl(table.getCanonicalName(), table.getName()), index, indexName, reason);
                        throw new DuplicateRelationNameException(message);
                    }
                }
            }
        }

        public void write(DataOutputStream outDB) throws IOException {
            outDB.write('v' + version);  //для поддержки обратной совместимости
            outDB.writeUTF(migrationVersion.toString());

            //записываем список подключенных модулей
            outDB.writeInt(businessLogics.getLogicModules().size());
            for (LogicsModule logicsModule : businessLogics.getLogicModules())
                outDB.writeUTF(logicsModule.getName());

            outDB.writeInt(tables.size());
            for (Map.Entry<DBTable, List<IndexData<Field>>> tableIndexes : tables.entrySet()) {
                tableIndexes.getKey().serialize(outDB);
                outDB.writeInt(tableIndexes.getValue().size());
                for (IndexData<Field> index : tableIndexes.getValue()) {
                    outDB.writeInt(index.fields.size());
                    for (Field indexField : index.fields) {
                        outDB.writeUTF(indexField.getName());
                    }
                    index.options.serialize(outDB);
                }
            }

            outDB.writeInt(storedProperties.size());
            for (DBStoredProperty property : storedProperties) {
                outDB.writeUTF(property.getCanonicalName());
                outDB.writeUTF(property.getDBName());
                outDB.writeBoolean(property.isDataProperty);
                outDB.writeUTF(property.tableName);
                for (int i=0,size=property.mapKeys.size();i<size;i++) {
                    outDB.writeInt(property.mapKeys.getKey(i));
                    outDB.writeUTF(property.mapKeys.getValue(i).getName());
                }
            }
        }
    }

    private static class MoveDBProperty {

        public final DBStoredProperty newProperty;
        public final DBStoredProperty oldProperty;
        public final ImRevMap<KeyField, KeyField> mapKeys;

        public MoveDBProperty(DBStoredProperty newProperty, DBStoredProperty oldProperty, ImRevMap<KeyField, KeyField> mapKeys) {
            this.newProperty = newProperty;
            this.oldProperty = oldProperty;
            this.mapKeys = mapKeys;
        }
    }

    public static int oldDBStructureVersion = 0;
    public static int newDBStructureVersion = 41;

    private class OldDBStructure extends DBStructure<String> {

        public OldDBStructure(DataInputStream inputDB) throws IOException {
            migrationVersion = new MigrationVersion("0.0");
            if (inputDB == null) {
                version = -2;
            } else {
                version = readOldDBVersion(inputDB);
                oldDBStructureVersion = version;
                migrationVersion = new MigrationVersion(inputDB.readUTF());

                int modulesCount = inputDB.readInt();
                if (modulesCount > 0) {
                    for (int i = 0; i < modulesCount; i++)
                        modulesList.add(inputDB.readUTF());
                }

                for (int i = inputDB.readInt(); i > 0; i--) {
                    SerializedTable prevTable;
                    if (version < 38) {
                        prevTable = new SerializedTable(inputDB.readUTF(), null, inputDB, LM.baseClass);
                    } else {
                        prevTable = deserializeTable(inputDB, LM.baseClass);
                    }
                    List<IndexData<String>> indexes = new ArrayList<>();
                    for (int j = inputDB.readInt(); j > 0; j--) {
                        List<String> index = new ArrayList<>();
                        for (int k = inputDB.readInt(); k > 0; k--) {
                            index.add(inputDB.readUTF());
                        }
                        if (version < 32) {
                            indexes.add(new IndexData<>(index, IndexOptions.deserialize32(inputDB)));
                        } else if (version < 36) {
                            indexes.add(new IndexData<>(index, IndexOptions.deserialize35(inputDB)));
                        } else {
                            indexes.add(new IndexData<>(index, IndexOptions.deserialize(inputDB)));
                        }

                    }
                    tables.put(prevTable, indexes);
                }

                int prevStoredNum = inputDB.readInt();
                for (int i = 0; i < prevStoredNum; i++) {
                    String canonicalName = inputDB.readUTF();
                    String dbName = inputDB.readUTF();
                    boolean isDataProperty = inputDB.readBoolean();
                    
                    String tableName = inputDB.readUTF();
                    DBTable prevTable = getTable(tableName);
                    MExclMap<Integer, KeyField> mMapKeys = MapFact.mExclMap(prevTable.getTableKeys().size());
                    for (int j = 0; j < prevTable.getTableKeys().size(); j++) {
                        mMapKeys.exclAdd(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));
                    }
                    storedProperties.add(new DBStoredProperty(canonicalName, dbName, isDataProperty, tableName, mMapKeys.immutable()));
                }

                int prevConcreteNum = inputDB.readInt();
                for(int i = 0; i < prevConcreteNum; i++)
                    concreteClasses.add(new DBConcreteClass(inputDB.readUTF(), inputDB.readUTF(), inputDB.readLong()));
            }
        }
    
        private SerializedTable deserializeTable(DataInputStream inStream, BaseClass baseClass) throws IOException {
            String dbName = inStream.readUTF();
            String canonicalName = null;
            if (inStream.readBoolean()) {
                canonicalName = inStream.readUTF();
            }
            return new SerializedTable(dbName, canonicalName, inStream, baseClass);
        }
    
        boolean isEmpty() {
            return version < 0;
        }
    }


    public void setDefaultUserLanguage(String defaultUserLanguage) {
        this.defaultUserLanguage = defaultUserLanguage;
    }

    public void setDefaultUserCountry(String defaultUserCountry) {
        this.defaultUserCountry = defaultUserCountry;
    }

    public void setDefaultUserTimezone(String defaultUserTimezone) {
        this.defaultUserTimezone = defaultUserTimezone;
    }

    public void setDefaultUserTwoDigitYearStart(Integer defaultUserTwoDigitYearStart) {
        this.defaultUserTwoDigitYearStart = defaultUserTwoDigitYearStart;
    }

    public void setDefaultUserDateFormat(String defaultUserDateFormat) {
        this.defaultUserDateFormat = defaultUserDateFormat;
    }

    public void setDefaultUserTimeFormat(String defaultUserTimeFormat) {
        this.defaultUserTimeFormat = defaultUserTimeFormat;
    }
}
