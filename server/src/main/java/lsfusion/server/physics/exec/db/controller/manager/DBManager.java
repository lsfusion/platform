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
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.base.col.lru.LRUWWEVSMap;
import lsfusion.base.file.RawFileData;
import lsfusion.base.lambda.DProcessor;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.ProgressBar;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.base.controller.stack.*;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.query.result.ReadBatchResultHandler;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.MigrationScriptLexer;
import lsfusion.server.language.MigrationScriptParser;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.controller.init.SessionCreator;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ByteArrayClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.interactive.property.PropertyAsync;
import lsfusion.server.logics.navigator.controller.env.*;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
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

import static java.util.Arrays.asList;
import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.SystemUtils.getRevision;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.directLI;

public class DBManager extends LogicsManager implements InitializingBean {
    public static final Logger logger = Logger.getLogger(DBManager.class);
    public static final Logger startLogger = ServerLoggers.startLogger;
    public static final Logger serviceLogger = ServerLoggers.serviceLogger;

    private Map<String, String> finalPropertyDrawNameChanges = new HashMap<>();
    private Map<String, String> finalNavigatorElementNameChanges = new HashMap<>();

    private DataAdapter adapter;

    private RestartManager restartManager;

    private MigrationManager migrationManager;
    
    private BusinessLogics businessLogics;

    private boolean ignoreMigration;
    private boolean migrationScriptWasRead = false;

    private boolean denyDropModules;

    private boolean denyDropTables;

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

    public void addIndex(Property property) {
        addIndex(new LP(property));
    }

    public void addIndex(LP lp) {
        addIndex(lp, IndexType.DEFAULT);
    }

    public void addIndex(LP lp, IndexType indexType) {
        ImOrderSet<String> keyNames = SetFact.toOrderExclSet(lp.listInterfaces.size(), i -> "key"+i);
        addIndex(keyNames, indexType, directLI(lp));
    }

    public void addIndex(ImOrderSet<String> keyNames, Object... params) {
        addIndex(keyNames, IndexType.DEFAULT, params);
    }

    public void addIndex(ImOrderSet<String> keyNames, IndexType indexType, Object... params) {
        ImList<PropertyObjectInterfaceImplement<String>> index = ActionOrPropertyUtils.readObjectImplements(keyNames, params);
        addIndex(index, indexType);
    }

    public void initReflectionEvents() {
        assert LM == null && reflectionLM == null;
        LM = businessLogics.LM;
        reflectionLM = businessLogics.reflectionLM;
        systemEventsLM = businessLogics.systemEventsLM;

        try {
            SQLSession sql = getThreadLocalSql();

            adapter.ensure(false);

            if (!isFirstStart(sql)) {
                updateReflectionStats(sql);

                startLogger.info("Setting user logging for properties");
                setUserLoggableProperties(sql);

                startLogger.info("Setting user not null constraints for properties");
                setNotNullProperties(sql);

                if (getOldDBStructure(sql).version >= 34) {
                    startLogger.info("Disabling input list");
                    setDisableInputListProperties(sql);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private void setUserLoggableProperties(SQLSession sql) throws SQLException, SQLHandledException {
        Map<String, String> changes = businessLogics.getDbManager().getPropertyCNChanges(sql);
        
        Integer maxStatsProperty = null;
        try {
            maxStatsProperty = (Integer) reflectionLM.maxStatsProperty.read(sql, Property.defaultModifier, DataSession.emptyEnv(OperationOwner.unknown));
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

    public static Integer getPropertyInterfaceStat(Property property) {
        Integer statsProperty = null;
        Stat interfaceStat = property.getInterfaceStat();
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
            if(prop != null)
                LM.setNotNull(prop, ListFact.EMPTY());
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

    public void checkIndices(SQLSession session) throws SQLException, SQLHandledException {
        try {
            for (Map.Entry<NamedTable, Map<List<Field>, IndexOptions>> mapIndex : getIndicesMap().entrySet()) {
                session.startTransaction(START_TIL, OperationOwner.unknown);
                NamedTable table = mapIndex.getKey();
                for (Map.Entry<List<Field>, IndexOptions> index : mapIndex.getValue().entrySet()) {
                    ImOrderSet<Field> fields = SetFact.fromJavaOrderSet(index.getKey());
                    if (!getThreadLocalSql().checkIndex(table, table.keys, fields, index.getValue()))
                        session.addIndex(table, table.keys, fields, index.getValue(), BusinessLogics.sqlLogger, true);
                }
                session.addConstraint(table);
                session.checkExtraIndices(getThreadLocalSql(), table, table.keys, BusinessLogics.sqlLogger);
                session.commitTransaction();
            }
        } catch (Exception e) {
            session.rollbackTransaction();
            throw e;
        }
    }

    public void firstRecalculateStats(DataSession session) throws SQLException, SQLHandledException {
        if(reflectionLM.hasNotNullQuantity.read(session) == null) {
            recalculateStats(session);
            session.applyException(businessLogics, ThreadLocalContext.getStack());
        }
    }

    private void recalculateClassStats(DataSession session, boolean log) throws SQLException, SQLHandledException {
        for (ObjectValueClassSet tableClasses : LM.baseClass.getUpObjectClassFields().valueIt()) {
            recalculateClassStat(LM, tableClasses, session, log);
        }
    }

    private ImMap<Long, Integer> recalculateClassStat(BaseLogicsModule LM, ObjectValueClassSet tableClasses, DataSession session, boolean log) throws SQLException, SQLHandledException {
        long start = System.currentTimeMillis();
        if(log)
            BaseUtils.serviceLogger.info(String.format("Recalculate Class Stats: %s", String.valueOf(tableClasses)));

        QueryBuilder<Integer, Integer> classes = new QueryBuilder<>(SetFact.singleton(0));
        KeyExpr countKeyExpr = new KeyExpr("count");
        Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(LM.baseClass)),
                ValueExpr.COUNT, countKeyExpr.isClass(tableClasses), GroupType.SUM, classes.getMapExprs());
        classes.addProperty(0, countExpr);
        classes.and(countExpr.getWhere());
        ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);

        ImSet<ConcreteCustomClass> concreteChilds = tableClasses.getSetConcreteChildren();
        MExclMap<Long, Integer> mResult = MapFact.mExclMap(concreteChilds.size());
        for (int i = 0, size = concreteChilds.size(); i < size; i++) {
            ConcreteCustomClass customClass = concreteChilds.get(i);
            ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, customClass.ID));
            int statValue = classStat == null ? 1 : (Integer) classStat.singleValue();
            mResult.exclAdd(customClass.ID, statValue);
            LM.statCustomObjectClass.change(statValue, session, customClass.getClassObject());
        }
        long time = System.currentTimeMillis() - start;
        if(log)
            BaseUtils.serviceLogger.info(String.format("Recalculate Class Stats: %s, %sms", String.valueOf(tableClasses), time));
        return mResult.immutable();
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

    private ImMap<String, Integer> updateReflectionStats(SQLSession sql) throws SQLException, SQLHandledException {
        return updateStats(sql, true);
    }
    
    private ImMap<String, Integer> updateStats(SQLSession sql, boolean useSIDsForClasses) throws SQLException, SQLHandledException {
        ImMap<String, Integer> result = updateTableStats(sql, true); // чтобы сами таблицы статистики получили статистику
        updateFullClassStats(sql, useSIDsForClasses);
        if(SystemProperties.doNotCalculateStats)
            return result;
        return updateTableStats(sql, false);
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

    private ImMap<String, Integer> updateTableStats(SQLSession sql, boolean statDefault) throws SQLException, SQLHandledException {
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
            dataTable.updateStat(tableStats, keyStats, propStats, null, statDefault);
        }
        return tableStats;
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
            BaseUtils.serviceLogger.info(String.format("Recalculate Stats %s of %s: %s", count, tables.size(), String.valueOf(dataTable)));
            dataTable.recalculateStat(reflectionLM, getDisableStatsTableColumnSet(), session);
            long time = System.currentTimeMillis() - start;
            BaseUtils.serviceLogger.info(String.format("Recalculate Stats: %s, %sms", String.valueOf(dataTable), time));
        }
        recalculateClassStats(session, true);
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

    public String checkClasses(SQLSession session) throws SQLException, SQLHandledException {
        String message = DataSession.checkClasses(session, LM.baseClass);
        for(ImplementTable implementTable : LM.tableFactory.getImplementTables()) {
            message += DataSession.checkTableClasses(implementTable, session, LM.baseClass, false); // так как снизу есть проверка классов
        }
        ImOrderSet<Property> storedDataProperties;
        try(DataSession dataSession = createRecalculateSession(session)) {
            storedDataProperties = businessLogics.getStoredDataProperties(dataSession);
        }
        for (Property property : storedDataProperties)
            message += DataSession.checkClasses(property, session, LM.baseClass);
        return message;
    }

    public void recalculateExclusiveness(final SQLSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        run(session, isolatedTransactions, sql -> DataSession.runExclusiveness(query -> {
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
                removeClasses.getKey(i).dataProperty.dropInconsistentClasses(session, LM.baseClass, key, where, OperationOwner.unknown);
            }
        }, sql, LM.baseClass));
    }

    public String recalculateClasses(SQLSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        recalculateExclusiveness(session, isolatedTransactions);

        final List<String> messageList = new ArrayList<>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        for (final ImplementTable implementTable : LM.tableFactory.getImplementTables()) {
            run(session, isolatedTransactions, sql -> {
                long start = System.currentTimeMillis();
                DataSession.recalculateTableClasses(implementTable, sql, LM.baseClass);
                long time = System.currentTimeMillis() - start;
                String message = String.format("Recalculate Table Classes: %s, %sms", implementTable.toString(), time);
                BaseUtils.serviceLogger.info(message);
                if (time > maxRecalculateTime)
                    messageList.add(message);
            });
        }

        try(DataSession dataSession = createRecalculateSession(session)) {
            for (final Property property : businessLogics.getStoredDataProperties(dataSession))
                run(session, isolatedTransactions, sql -> {
                    long start = System.currentTimeMillis();
                    property.recalculateClasses(sql, LM.baseClass);
                    long time = System.currentTimeMillis() - start;
                    String message = String.format("Recalculate Class: %s, %sms", property.getSID(), time);
                    BaseUtils.serviceLogger.info(message);
                    if (time > maxRecalculateTime) messageList.add(message);
                });
            return businessLogics.formatMessageList(messageList);
        }
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

    public void setDenyDropModules(boolean denyDropModules) {
        this.denyDropModules = denyDropModules;
    }

    public void setDenyDropTables(boolean denyDropTables) {
        this.denyDropTables = denyDropTables;
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

    public void updateStats(SQLSession sql) throws SQLException, SQLHandledException {
        updateStats(sql, false);
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

    @Override
    protected void onInit(LifecycleEvent event) {
        this.LM = businessLogics.LM;
        this.reflectionLM = businessLogics.reflectionLM;
        this.systemEventsLM = businessLogics.systemEventsLM;
        try {
            if(getSyntax().getSyntaxType() == SQLSyntaxType.MSSQL)
                Expr.useCasesCount = 5;

            startLogger.info("Synchronizing DB");
            synchronizeDB();
        } catch (Exception e) {
            throw new RuntimeException("Error synchronizing DB: ", e);
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

    private SQLSessionContextProvider contextProvider = new SQLSessionContextProvider() {
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
    };

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
                () -> 0,
                new ChangesController() {
                    public DBManager getDbManager() {
                        return DBManager.this;
                    }
                }, Locale::getDefault, upOwner
        );
    }

    public DataSession createSession(SQLSession sql, UserController userController, FormController formController,
                                     TimeoutController timeoutController, ChangesController changesController, LocaleController localeController, OperationOwner owner) throws SQLException {
        //todo: неплохо бы избавиться от зависимости на restartManager, а то она неестественна
        return new DataSession(sql, userController, formController, timeoutController, changesController, localeController, () -> restartManager.isPendingRestart(),
                               LM.baseClass, businessLogics.systemEventsLM.session, businessLogics.systemEventsLM.currentSession, getIDSql(), businessLogics.getSessionEvents(), owner);
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
                return new DataObject(addObject.object, businessLogics.authenticationLM.computer); // to update classes after apply
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
        for (NamedTable table : oldDBStructure.tables.keySet()) {
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
    private static class Param {
        public final ImMap<?, ObjectValue> mapValues;
        public final String value;
        public final boolean objects;

        public Param(ImMap<?, ObjectValue> mapValues, String value, boolean objects) {
            this.mapValues = mapValues;
            this.value = value;
            this.objects = objects;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof Param && mapValues.equals(((Param) o).mapValues) && value.equals(((Param) o).value) && objects == ((Param) o).objects;
        }

        public int hashCode() {
            return 31 * (mapValues.hashCode() * 31 + value.hashCode()) + (objects ? 1 : 0);
        }
    }
    private static class ParamRef {
        public Param param;

        public ParamRef(Param param) {
            this.param = param;
        }

        private void drop() {
            param = null;
        }
    }
    private static class ValueRef<P extends PropertyInterface> {
        // it's the only strong value to keep it from garbage collected
        public final ParamRef ref;
        public final PropertyAsync<P>[] values;

        public ValueRef(ParamRef ref, PropertyAsync<P>[] values) {
            this.ref = ref;
            this.values = values;
        }
    }

    // need this to clean outdated caches
    // could be done easier with timestamps (value and last changed), but this way cache will be cleaned faster
    private final LRUWVSMap<Property<?>, ConcurrentIdentityWeakHashSet<ParamRef>> asyncValuesPropCache1 = new LRUWVSMap<>(LRUUtil.G1);
    private final LRUWVSMap<Property<?>, ConcurrentIdentityWeakHashSet<ParamRef>> asyncValuesPropCache2 = new LRUWVSMap<>(LRUUtil.G2);

    // we need weak ref, but regular equals (not identity)
    private final LRUWWEVSMap<Property<?>, Param, ValueRef> asyncValuesValueCache1 = new LRUWWEVSMap<>(LRUUtil.G1);
    private final LRUWWEVSMap<Property<?>, Param, ValueRef> asyncValuesValueCache2 = new LRUWWEVSMap<>(LRUUtil.G2);

    public <P extends PropertyInterface> PropertyAsync<P>[] getAsyncValues(InputValueList<P> list, String value, AsyncMode mode) throws SQLException, SQLHandledException {
        if(Settings.get().isIsClustered()) // we don't want to use caches since they can be inconsistent
            return readAsyncValues(list, value, mode);

        LRUWVSMap<Property<?>, ConcurrentIdentityWeakHashSet<ParamRef>> asyncValuesPropCache;
        LRUWWEVSMap<Property<?>, Param, ValueRef> asyncValuesValueCache;
        if(value.length() >= Settings.get().getAsyncValuesLongCacheThreshold() || !list.mapValues.isEmpty()) {
            asyncValuesPropCache = asyncValuesPropCache1;
            asyncValuesValueCache = asyncValuesValueCache1;
        } else {
            asyncValuesPropCache = asyncValuesPropCache2;
            asyncValuesValueCache = asyncValuesValueCache2;
        }

        // strict param is transient (it is used only for optimization purposes, so we won't use it in cache)
        Param param = new Param(list.mapValues, value, mode == AsyncMode.OBJECTS);

        ValueRef<P> valueRef = asyncValuesValueCache.get(list.property, param);
        if(valueRef != null && valueRef.ref.param != null)
            return valueRef.values;

        // has to be before reading to be sure that ref will be dropped when changes are made
        ParamRef ref = new ParamRef(param);
        ConcurrentIdentityWeakHashSet<ParamRef> paramRefs = asyncValuesPropCache.get(list.property);
        if(paramRefs == null) {
            paramRefs = new ConcurrentIdentityWeakHashSet<>();
            asyncValuesPropCache.put(list.property, paramRefs);
        }
        paramRefs.add(ref);

        PropertyAsync<P>[] values = readAsyncValues(list, value, mode);
        asyncValuesValueCache.put(list.property, param, new ValueRef<>(ref, values));

        return values;
    }

    private <P extends PropertyInterface> PropertyAsync<P>[] readAsyncValues(InputValueList<P> list, String value, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        PropertyAsync<P>[] values;
        try(DataSession session = createSession()) {
            values = FormInstance.getAsyncValues(list, session, Property.defaultModifier, value, asyncMode);
        }
        return values;
    }

    public void flushChanges() {
        ImSet<Property> flushedChanges;
        synchronized (changesListLock) {
            flushedChanges = mChanges.immutable();
        }
        if(flushedChanges.isEmpty())
            return;

        FunctionSet<Property> changedSet = Property.getDependsOnSet(flushedChanges);
        DProcessor<Property<?>, ConcurrentIdentityWeakHashSet<ParamRef>> dropCaches = (property, refs) -> {
            if (property != null && changedSet.contains(property)) {
                for (ParamRef ref : refs)
                    ref.drop(); // dropping ref will eventually lead to garbage collection of entries in all lru caches, plus there is a check that cache is dropped
            }
        };
        asyncValuesPropCache1.proceedSafeLockLRUEKeyValues(dropCaches);
        asyncValuesPropCache2.proceedSafeLockLRUEKeyValues(dropCaches);
    }

    private final Object changesListLock = new Object();
    private MSet<Property> mChanges = SetFact.mSet();

    public void registerChange(ImSet<Property> properties) {
        synchronized (changesListLock) {
            mChanges.addAll(properties);
        }
    }

    private static ImMap<String, ImRevMap<String, String>> getFieldToCanMap(DBStructure<?> dbStructure) {
        return SetFact.fromJavaOrderSet(dbStructure.storedProperties).getSet().group(new BaseUtils.Group<String, DBStoredProperty>() {
            public String group(DBStoredProperty value) {
                return value.tableName;
            }}).mapValues(value -> value.mapRevKeyValues(DBStoredProperty::getDBName, DBStoredProperty::getCanonicalName));

    }

    // Удаляем несуществующие индексы и убираем из newDBStructure не изменившиеся индексы
    // Делаем это до применения migration script, то есть не пытаемся сохранить все возможные индексы по максимуму
    private void checkIndices(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {

        ImMap<String, String> propertyChanges = MapFact.fromJavaMap(migrationManager.getStoredPropertyCNChangesAfter(oldDBStructure.migrationVersion));

        ImMap<String, ImRevMap<String, String>> oldTableFieldToCan = MapFact.EMPTY(); ImMap<String, ImRevMap<String, String>> newTableFieldToCan = MapFact.EMPTY();
        if(!propertyChanges.isEmpty()) { // оптимизация
            oldTableFieldToCan = getFieldToCanMap(oldDBStructure);
            newTableFieldToCan = getFieldToCanMap(newDBStructure);
        }

        for (Map.Entry<NamedTable, Map<List<String>, IndexOptions>> oldTableIndices : oldDBStructure.tables.entrySet()) {
            NamedTable oldTable = oldTableIndices.getKey();
            NamedTable newTable = newDBStructure.getTable(oldTable.getName());
            Map<List<Field>, IndexOptions> newTableIndices = null; Map<List<String>, Pair<IndexOptions, List<Field>>> newTableIndicesNames = null; ImMap<String, String> fieldOldToNew = MapFact.EMPTY();
            if(newTable != null) {
                newTableIndices = newDBStructure.tables.get(newTable);
                newTableIndicesNames = new HashMap<>();
                for (Map.Entry<List<Field>, IndexOptions> entry : newTableIndices.entrySet()) {
                    List<String> names = new ArrayList<>();
                    for (Field field : entry.getKey())
                        names.add(field.getName());
                    newTableIndicesNames.put(names, new Pair<>(entry.getValue(), entry.getKey()));
                }

                // old field -> old cn -> new cn -> ne field
                if(!propertyChanges.isEmpty()) {
                    ImRevMap<String, String> oldFieldToCan = oldTableFieldToCan.get(oldTable.getName());
                    ImRevMap<String, String> newFieldToCan = newTableFieldToCan.get(newTable.getName());
                    if(oldFieldToCan != null && newFieldToCan != null) // так как таблицы могут быть пустыми
                        fieldOldToNew = oldFieldToCan.innerJoin(propertyChanges).innerCrossValues(newFieldToCan);
                }
            }

            for (Map.Entry<List<String>, IndexOptions> oldIndex : oldTableIndices.getValue().entrySet()) {
                List<String> oldIndexKeys = oldIndex.getKey();
                ImOrderSet<String> oldIndexKeysSet = SetFact.fromJavaOrderSet(oldIndexKeys);

                boolean replaced = BaseUtils.replaceListElements(oldIndexKeys, fieldOldToNew);

                boolean drop = (newTable == null); // ушла таблица
                if (!drop) {
                    Pair<IndexOptions, List<Field>> newIndex = newTableIndicesNames.get(oldIndexKeys);
                    if (newIndex != null && newIndex.first.equals(oldIndex.getValue())) {
                        newTableIndices.remove(newIndex.second); // не трогаем индекс
                    } else {
                        drop = true;
                    }
                }

                if (drop) {
                    sql.dropIndex(oldTable, oldTable.keys, oldIndexKeysSet, oldIndex.getValue(), Settings.get().isStartServerAnyWay());
                } else {
                    if(replaced) // assert что keys совпадают
                        sql.renameIndex(oldTable, oldTable.keys, oldIndexKeysSet, SetFact.fromJavaOrderSet(oldIndexKeys), oldIndex.getValue(), Settings.get().isStartServerAnyWay());
                }
            }
        }
    }

    private void checkUniqueDBName(NewDBStructure struct) {
        Map<Pair<String, String>, DBStoredProperty> sids = new HashMap<>();
        for (DBStoredProperty property : struct.storedProperties) {
            Pair<String, String> key = new Pair<>(property.getDBName(), property.getTable().getName());
            if (sids.containsKey(key)) {
                startLogger.error(String.format("Equal sid '%s' in table '%s': %s and %s", key.first, key.second, sids.get(key).getCanonicalName(), property.getCanonicalName()));
            }
            sids.put(key, property);
         }
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
                run(sql, isolatedTransactions, sql1 -> uploadTableToDB(sql1, implementTable, fi + "/" + size, sqlFrom, owner));
            }
        } finally {
            sql.popNoQueryLimit();
        }
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

        public IDAdd(long object, ConcreteCustomClass customClass, String sID, String caption) {
            this.object = object;
            this.customClass = customClass;
            this.sID = sID;
            this.caption = caption;
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

        public void apply(DataSession session, BaseLogicsModule LM, boolean isFirstStart) throws SQLException, SQLHandledException {
            LM.fillingIDs.change(true, session); // need this to avoid constraint on staticName changing

            for (IDAdd addedObject : added) {
                if(!isFirstStart)
                    startLogger.info("adding static object with id " + addedObject.object + ", sid " + addedObject.sID + " and name " + addedObject.caption);
                DataObject classObject = new DataObject(addedObject.object, LM.baseClass.unknown);
                session.changeClass(classObject, addedObject.customClass);
                LM.staticName.change(addedObject.sID, session, classObject);
                LM.staticCaption.change(addedObject.caption, session, classObject);
            }

            for (Map.Entry<DataObject, String> modifiedSID : modifiedSIDs.entrySet()) {
                startLogger.info("changing sid of static object with id " + modifiedSID.getKey() + " to " + modifiedSID.getValue());
                LM.staticName.change(modifiedSID.getValue(), session, modifiedSID.getKey());
            }

            for (Map.Entry<DataObject, String> modifiedCaption : modifiedCaptions.entrySet()) {
                startLogger.info("renaming static object with id " + modifiedCaption.getKey() + " to " + modifiedCaption.getValue());
                LM.staticCaption.change(modifiedCaption.getValue(), session, modifiedCaption.getKey());
            }

            for (IDRemove removedObject : removed) {
                startLogger.info("removing static object with id " + removedObject.object.object + " and sid " + removedObject.sID);
                session.changeClass(removedObject.object, LM.baseClass.unknown);
            }
        }
    }

    public void synchronizeDB() throws Exception {

        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // потом надо сделать соответствующий механизм для Formula
        ScriptingLogicsModule module = businessLogics.getModule("Country");
        if(module != null) {
            LP<?> lp = module.findProperty("isDayOff[Country,DATE]");

            Properties props = new Properties();
            props.put("dayoff.tablename", lp.property.mapTable.table.getName(sql.syntax));
            props.put("dayoff.fieldname", lp.property.field.getName(sql.syntax));
            adapter.ensureScript("jumpWorkdays.tsql", props);
        }

        SQLSyntax syntax = getSyntax();

        // "старое" состояние базы
        OldDBStructure oldDBStructure = getOldDBStructure(sql);
        if(!oldDBStructure.isEmpty() && oldDBStructure.version < 30)
            throw new RuntimeException("You should update to version 30 first");
        checkModules(oldDBStructure);

        // В этот момент в обычной ситуации migration script уже был обработан, вызов оставлен на всякий случай. Повторный вызов ничего не делает.
        runMigrationScript();
        migrationManager.checkMigrationVersion(oldDBStructure.migrationVersion);

        Map<String, String> columnsToDrop = new HashMap<>();

        boolean noTransSyncDB = Settings.get().isNoTransSyncDB();

        try {
            sql.pushNoHandled();

            if(noTransSyncDB)
                sql.startFakeTransaction(OperationOwner.unknown);
            else
                sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);

            // новое состояние базы
            ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
            DataOutputStream outDB = new DataOutputStream(outDBStruct);

            MigrationVersion newMigrationVersion = migrationManager.getCurrentMigrationVersion(oldDBStructure.migrationVersion);
            NewDBStructure newDBStructure = new NewDBStructure(newMigrationVersion);

            checkUniqueDBName(newDBStructure);
            // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
            newDBStructure.write(outDB);

            startLogger.info("Checking indices");

            checkIndices(sql, oldDBStructure, newDBStructure);

            if (!oldDBStructure.isEmpty()) {
                startLogger.info("Applying migration script (" + oldDBStructure.migrationVersion + " -> " + newDBStructure.migrationVersion + ")");

                // применяем к oldDBStructure изменения из migration script, переименовываем таблицы и поля
                alterDBStructure(oldDBStructure, newDBStructure, sql);
            }

            // проверка, не удалятся ли старые таблицы
            if (denyDropTables) {
                String droppedTables = getDroppedTablesString(sql, oldDBStructure, newDBStructure);
                if (!droppedTables.isEmpty()) {
                    throw new RuntimeException("Dropped tables: " + droppedTables);
                }
            }

            // добавим таблицы которых не было
            startLogger.info("Creating tables");
            for (NamedTable table : newDBStructure.tables.keySet()) {
                if (oldDBStructure.getTable(table.getName()) == null)
                    sql.createTable(table, table.keys, startLogger);
            }

            // проверяем изменение структуры ключей
            for (NamedTable table : newDBStructure.tables.keySet()) {
                NamedTable oldTable = oldDBStructure.getTable(table.getName());
                if (oldTable != null) {
                    for (KeyField key : table.keys) {
                        KeyField oldKey = oldTable.findKey(key.getName());
                        if(oldKey == null)
                            throw new RuntimeException("Key " + key + " is not found in table : " + oldTable + ". New table : " + table);
                        if (!(key.type.equals(oldKey.type))) {
                            startLogger.info("Changing type of key column " + key + " in table " + table + " from " + oldKey.type + " to " + key.type);
                            sql.modifyColumn(table, key, oldKey.type);
                        }
                    }
                }
            }

            List<Property> recalculateProperties = new ArrayList<>();

            MExclSet<Pair<String, String>> mDropColumns = SetFact.mExclSet(); // вообще pend'ить нужно только classDataProperty, но их тогда надо будет отличать

            // бежим по свойствам
            List<DBStoredProperty> restNewDBStored = new LinkedList<>(newDBStructure.storedProperties);
            List<Property> recalculateStatProperties = new ArrayList<>();
            Map<ImplementTable, Map<Field, Type>> alterTableMap = new HashMap<>();
            for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
                NamedTable oldTable = oldDBStructure.getTable(oldProperty.tableName);

                boolean keep = false, moved = false;
                for (Iterator<DBStoredProperty> is = restNewDBStored.iterator(); is.hasNext(); ) {
                    DBStoredProperty newProperty = is.next();

                    if (newProperty.getCanonicalName().equals(oldProperty.getCanonicalName())) {
                        MExclMap<KeyField, PropertyInterface> mFoundInterfaces = MapFact.mExclMapMax(newProperty.property.interfaces.size());
                        for (PropertyInterface propertyInterface : newProperty.property.interfaces) {
                            KeyField mapKeyField = oldProperty.mapKeys.get(propertyInterface.ID);
                            if (mapKeyField != null)
                                mFoundInterfaces.exclAdd(mapKeyField, propertyInterface);
                        }
                        ImMap<KeyField, PropertyInterface> foundInterfaces = mFoundInterfaces.immutable();

                        if (foundInterfaces.size() == oldProperty.mapKeys.size()) { // если все нашли
                            ImplementTable newTable = newProperty.getTable();
                            if (!(keep = newProperty.tableName.equals(oldProperty.tableName))) { // если в другой таблице
                                sql.addColumn(newTable, newProperty.property.field);
                                // делаем запрос на перенос

                                startLogger.info(localize(LocalizedString.createFormatted("{logics.info.property.is.transferred.from.table.to.table}", newProperty.property.field, newProperty.property.caption, oldProperty.tableName, newProperty.tableName)));
                                newProperty.property.mapTable.table.moveColumn(sql, newProperty.property.field, oldTable,
                                        foundInterfaces.join((ImMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys), oldTable.findProperty(oldProperty.getDBName()));
                                startLogger.info("Done");
                                moved = true;
                                recalculateStatProperties.add(newProperty.property);
                            } else { // надо проверить что тип не изменился
                                Type oldType = oldTable.findProperty(oldProperty.getDBName()).type;
                                if (!oldType.equals(newProperty.property.field.type)) {
                                    startLogger.info("Prepare changing type of property column " + newProperty.property.field + " in table " + newProperty.tableName + " from " + oldType + " to " + newProperty.property.field.type);
                                    Map<Field, Type> fieldTypeMap = alterTableMap.get(newTable);
                                    if(fieldTypeMap == null)
                                        fieldTypeMap = new HashMap<>();
                                    fieldTypeMap.put(newProperty.property.field, oldType);
                                    alterTableMap.put(newTable, fieldTypeMap);
                                }
                            }
                            is.remove();
                        }
                        break;
                    }
                }
                if (!keep) {
                    if (oldProperty.isDataProperty && !moved) {
                        String newName = "_DELETED_" + oldProperty.getDBName();
                        ExConnection exConnection = sql.getConnection();
                        Connection connection = exConnection.sql;
                        Savepoint savepoint = null;
                        try {
                            savepoint = connection.setSavepoint();
                            String oldName = oldProperty.getDBName();
                            startLogger.info("Deleting column " + oldName + " " + "(renaming to " + newName + ")  in table " + oldProperty.tableName);
                            sql.renameColumn(oldProperty.getTableName(syntax), oldName, newName);
                            columnsToDrop.put(newName, oldProperty.tableName);
                        } catch (PSQLException e) { // колонка с новым именем уже существует
                            if(savepoint != null)
                                connection.rollback(savepoint);
                            mDropColumns.exclAdd(new Pair<>(oldTable.getName(syntax), oldProperty.getDBName()));
                        } finally {
                            sql.returnConnection(exConnection, OperationOwner.unknown);
                        }
                    } else
                        mDropColumns.exclAdd(new Pair<>(oldTable.getName(syntax), oldProperty.getDBName()));
                }
            }

            for (Map.Entry<ImplementTable, Map<Field, Type>> entry : alterTableMap.entrySet()) {
                startLogger.info("Changing type of property columns (" + entry.getValue().size() + ") in table " + entry.getKey().getName() + " started");
                sql.modifyColumns(entry.getKey(), entry.getValue());
                startLogger.info("Changing type of property columns (" + entry.getValue().size() + ") in table " + entry.getKey().getName() + " finished");
            }

            for (DBStoredProperty property : restNewDBStored) { // добавляем оставшиеся
                sql.addColumn(property.getTable(), property.property.field);
                if (!oldDBStructure.isEmpty()) // если все свойства "новые" то ничего перерасчитывать не надо
                    recalculateProperties.add(property.property);
            }

            // обработка изменений с классами
            MMap<String, ImMap<String, ImSet<Long>>> mToCopy = MapFact.mMap(AMap.addMergeMapSets()); // в какое свойство, из какого свойства - какой класс
            for (DBConcreteClass oldClass : oldDBStructure.concreteClasses) {
                for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                    if (oldClass.sID.equals(newClass.sID)) {
                        if (!(oldClass.sDataPropID.equals(newClass.sDataPropID))) // надо пометить перенос, и удаление
                            mToCopy.add(newClass.sDataPropID, MapFact.singleton(oldClass.sDataPropID, SetFact.singleton(oldClass.ID)));
                        break;
                    }
                }
            }
            ImMap<String, ImMap<String, ImSet<Long>>> toCopy = mToCopy.immutable();
            for (int i = 0, size = toCopy.size(); i < size; i++) { // перенесем классы, которые сохранились но изменили поле
                DBStoredProperty classProp = newDBStructure.getProperty(toCopy.getKey(i));
                NamedTable table = newDBStructure.getTable(classProp.tableName);

                QueryBuilder<KeyField, PropertyField> copyObjects = new QueryBuilder<>(table);
                Expr keyExpr = copyObjects.getMapExprs().singleValue();
                Where moveWhere = Where.FALSE();
                ImMap<String, ImSet<Long>> copyFrom = toCopy.getValue(i);
                CaseExprInterface mExpr = Expr.newCases(true, copyFrom.size());
                MSet<String> mCopyFromTables = SetFact.mSetMax(copyFrom.size());
                for (int j = 0, sizeJ = copyFrom.size(); j < sizeJ; j++) {
                    DBStoredProperty oldClassProp = oldDBStructure.getProperty(copyFrom.getKey(j));
                    Table oldTable = oldDBStructure.getTable(oldClassProp.tableName);
                    mCopyFromTables.add(oldClassProp.tableName);

                    Expr oldExpr = oldTable.join(MapFact.singleton(oldTable.getTableKeys().single(), keyExpr)).getExpr(oldTable.findProperty(oldClassProp.getDBName()));
                    Where moveExprWhere = Where.FALSE();
                    for (long prevID : copyFrom.getValue(j))
                        moveExprWhere = moveExprWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                    mExpr.add(moveExprWhere, oldExpr);
                    moveWhere = moveWhere.or(moveExprWhere);
                }
                copyObjects.addProperty(table.findProperty(classProp.getDBName()), mExpr.getFinal());
                copyObjects.and(moveWhere);

                startLogger.info(localize(LocalizedString.createFormatted("{logics.info.objects.are.transferred.from.tables.to.table}", classProp.tableName, mCopyFromTables.immutable().toString())));
                sql.modifyRecords(new ModifyQuery(table, copyObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
            }
            ImMap<String, ImSet<Long>> toClean = MapFact.mergeMaps(toCopy.values(), ASet.addMergeSet());
            for (int i = 0, size = toClean.size(); i < size; i++) { // удалим оставшиеся классы
                DBStoredProperty classProp = oldDBStructure.getProperty(toClean.getKey(i));
                NamedTable table = oldDBStructure.getTable(classProp.tableName);

                QueryBuilder<KeyField, PropertyField> dropClassObjects = new QueryBuilder<>(table);
                Where moveWhere = Where.FALSE();

                PropertyField oldField = table.findProperty(classProp.getDBName());
                Expr oldExpr = table.join(dropClassObjects.getMapExprs()).getExpr(oldField);
                for (long prevID : toClean.getValue(i))
                    moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                dropClassObjects.addProperty(oldField, Expr.NULL());
                dropClassObjects.and(moveWhere);

                startLogger.info(localize(LocalizedString.createFormatted("{logics.info.objects.are.removed.from.table}", classProp.tableName)));
                sql.updateRecords(new ModifyQuery(table, dropClassObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
            }

            MSet<ImplementTable> mPackTables = SetFact.mSet();
            for (Pair<String, String> dropColumn : mDropColumns.immutable()) {
                startLogger.info("Dropping column " + dropColumn.second + " from table " + dropColumn.first);
                sql.dropColumn(dropColumn.first, dropColumn.second, Settings.get().isStartServerAnyWay());
                ImplementTable table = (ImplementTable) newDBStructure.getTable(dropColumn.first);
                if (table != null) mPackTables.add(table);
            }

            // удаляем таблицы старые
            for (NamedTable table : oldDBStructure.tables.keySet()) {
                if (newDBStructure.getTable(table.getName()) == null) {
                    sql.dropTable(table);
                    startLogger.info("Table " + table + " has been dropped");
                }
            }

            startLogger.info("Packing tables");
            packTables(sql, mPackTables.immutable(), false); // упакуем таблицы

            // создадим индексы в базе
            startLogger.info("Adding indices");
            for (Map.Entry<NamedTable, Map<List<Field>, IndexOptions>> mapIndex : newDBStructure.tables.entrySet())
                for (Map.Entry<List<Field>, IndexOptions> index : mapIndex.getValue().entrySet()) {
                    NamedTable table = mapIndex.getKey();
                    sql.addIndex(table, table.keys, SetFact.fromJavaOrderSet(index.getKey()), index.getValue(), oldDBStructure.getTable(table.getName()) == null ? null : startLogger, Settings.get().isStartServerAnyWay()); // если таблица новая нет смысла логировать
                }

            startLogger.info("Filling static objects ids");
            IDChanges idChanges = new IDChanges();
            LM.baseClass.fillIDs(sql, DataSession.emptyEnv(OperationOwner.unknown), this::generateID, LM.staticCaption, LM.staticName,
                    migrationManager.getClassSIDChangesAfter(oldDBStructure.migrationVersion), 
                    migrationManager.getObjectSIDChangesAfter(oldDBStructure.migrationVersion), 
                    idChanges);

            for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                newClass.ID = newClass.customClass.ID;
            }

            try(DataSession session = createSession(OperationOwner.unknown)) { // apply in transaction
                if (oldDBStructure.isEmpty()) {
                    startLogger.info("Recalculate class stats");
                    recalculateClassStats(session, false);
                    apply(session);
                }

                startLogger.info("Updating stats");
                ImMap<String, Integer> tableStats = updateStats(sql, false);  // пересчитаем статистику

                startLogger.info("Writing static objects changes");
                idChanges.apply(session, LM, oldDBStructure.isEmpty());
                apply(session);

                startLogger.info("Migrating reflection properties and actions");
                migrateReflectionProperties(session, oldDBStructure);
                newDBStructure.writeConcreteClasses(outDB);

                try {
                    sql.insertRecord(StructTable.instance, MapFact.EMPTY(), MapFact.singleton(StructTable.instance.struct, new DataObject(new RawFileData(outDBStruct), ByteArrayClass.instance)), true, TableOwner.global, OperationOwner.unknown);
                } catch (Exception e) {
                    ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(StructTable.instance.struct, new DataObject(RawFileData.EMPTY, ByteArrayClass.instance));
                    sql.insertRecord(StructTable.instance, MapFact.EMPTY(), propFields, true, TableOwner.global, OperationOwner.unknown);
                }

                startLogger.info("Recalculating aggregations");
                recalculateAggregations(session, getStack(), sql, recalculateProperties, false, startLogger); // перерасчитаем агрегации
                apply(session);
                recalculateProperties.addAll(recalculateStatProperties);
                updateAggregationStats(session,recalculateProperties, tableStats);
            writeDroppedColumns(session, columnsToDrop);
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

    private void classForNameSQL() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Class.forName("com.informix.jdbc.IfxDriver");
            Class.forName("com.mysql.jdbc.Driver");
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
        LM.logicsCaption.change(isRedundantString(logicsCaption) ? null : logicsCaption, session);
        LM.topModule.change(isRedundantString(topModule) ? null : topModule, session);
        apply(session);
    }

    private void initSystemUser(DataSession session) throws SQLException, SQLHandledException {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
        query.and(query.getMapExprs().singleValue().isClass(businessLogics.authenticationLM.systemUser));
        ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.EMPTYORDER(), 1).keyOrderSet();
        if (rows.size() == 0) {
            systemUser = (Long) session.addObject(businessLogics.authenticationLM.systemUser).object;
            apply(session);
        } else
            systemUser = (Long) rows.single().get("key");

        serverComputer = (long) getComputer(SystemUtils.getLocalHostName(), session, getStack()).object;
    }

    private void updateAggregationStats(DataSession session, List<Property> recalculateProperties, ImMap<String, Integer> tableStats) throws SQLException, SQLHandledException {
        Map<ImplementTable, List<Property>> propertiesMap; // статистика для новых свойств
        if (Settings.get().isGroupByTables()) {
            propertiesMap = new HashMap<>();
            for (Property property : recalculateProperties) {
                List<Property> entry = propertiesMap.get(property.mapTable.table);
                if (entry == null)
                    entry = new ArrayList<>();
                entry.add(property);
                propertiesMap.put(property.mapTable.table, entry);
            }
            for(Map.Entry<ImplementTable, List<Property>> entry : propertiesMap.entrySet())
                recalculateAndUpdateStat(session, entry.getKey(), entry.getValue());
        }
    }

    private void recalculateAndUpdateStat(DataSession session, ImplementTable table, List<Property> properties) throws SQLException, SQLHandledException {
        ImMap<PropertyField, String> fields = null;
        ImSet<PropertyField> skipRecalculateFields = null;
        if(properties != null) {
            ImSet<Property> propertySet = SetFact.fromJavaOrderSet(properties).getSet();
            fields = propertySet.mapKeyValues(value -> value.field, ActionOrProperty::getCanonicalName);
            skipRecalculateFields = propertySet.filterFn(property -> property instanceof StoredDataProperty).mapSetValues(property -> property.field);
        }
        long start = System.currentTimeMillis();
        startLogger.info(String.format("Update Aggregation Stats started: %s", table));
        
        table.recalculateStat(reflectionLM, session, fields, skipRecalculateFields, false);
        ImplementTable.CalcStat calculateStatResult = table.recalculateStat(reflectionLM, session, fields, skipRecalculateFields, true);
        apply(session);
        
        long time = System.currentTimeMillis() - start;
        startLogger.info(String.format("Update Aggregation Stats: %s, %sms", table, time));
            
        table.updateStat(MapFact.singleton(table.getName(), calculateStatResult.rows), calculateStatResult.keys, calculateStatResult.props, fields != null ? fields.keys() : null, false);
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

    public String checkAggregations(SQLSession session) throws SQLException, SQLHandledException {
        List<AggregateProperty> checkProperties;
        try(DataSession dataSession = createRecalculateSession(session)) {
            checkProperties = businessLogics.getRecalculateAggregateStoredProperties(dataSession, false);
        }
        String message = "";
        for (int i = 0; i < checkProperties.size(); i++) {
            Property property = checkProperties.get(i);
            if(property != null)
                message += ((AggregateProperty) property).checkAggregation(session, LM.baseClass, new ProgressBar(localize("{logics.info.checking.aggregated.property}"), i, checkProperties.size(), property.toString()));
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
    public String checkAggregationTableColumn(SQLSession session, String propertyCanonicalName) throws SQLException, SQLHandledException {
        Property property = businessLogics.getAggregateStoredProperty(propertyCanonicalName);
        return property != null ? ((AggregateProperty) property).checkAggregation(session, LM.baseClass) : null;
    }

    public String recalculateAggregations(ExecutionStack stack, SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        try(DataSession dataSession = createRecalculateSession(session)){
            List<String> messageList = recalculateAggregations(dataSession, stack, session, businessLogics.getRecalculateAggregateStoredProperties(dataSession, false), isolatedTransaction, serviceLogger);
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

    public static void run(SQLSession session, boolean runInTransaction, RunService run) throws SQLException, SQLHandledException {
        run(session, runInTransaction, run, 0);
    }

    private static void run(SQLSession session, boolean runInTransaction, RunService run, int attempts) throws SQLException, SQLHandledException {
        if(runInTransaction) {
            session.startTransaction(RECALC_TIL, OperationOwner.unknown);
            try {
                run.run(session);
                session.commitTransaction();
            } catch (Throwable t) {
                session.rollbackTransaction();
                if(t instanceof SQLHandledException && ((SQLHandledException)t).repeatApply(session, OperationOwner.unknown, attempts)) { // update conflict или deadlock или timeout - пробуем еще раз
                    //serviceLogger.error("Run error: ", t);
                    run(session, true, run, attempts + 1);
                    return;
                }

                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            }

        } else
            run.run(session);
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
                    boolean recalculate = reflectionLM.disableAggregationsTableColumn.read(dataSession, reflectionLM.tableColumnSID.readClasses(dataSession, new DataObject(property.getDBName()))) == null;
                    if(recalculate) {
                        properties.addAll(getDependentProperties(dataSession, prop, calculated, false));
                    }
                }
            }
        }
        return properties;
    }

    public List<String> recalculateAggregations(DataSession dataSession, ExecutionStack stack, SQLSession session, final List<? extends Property> recalculateProperties, boolean isolatedTransaction, Logger logger) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<>();
        final int total = recalculateProperties.size();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        if(total > 0) {
            for (int i = 0; i < recalculateProperties.size(); i++) {
                Property property = recalculateProperties.get(i);
                if(property instanceof AggregateProperty)
                    recalculateAggregation(dataSession, session, isolatedTransaction, new ProgressBar(localize("{logics.recalculation.aggregations}"), i, total), messageList, maxRecalculateTime, (AggregateProperty) property, logger);
            }
        }
        return messageList;
    }

    @StackProgress
    private void recalculateAggregation(final DataSession dataSession, SQLSession session, boolean isolatedTransaction, @StackProgress final ProgressBar progressBar, final List<String> messageList, final long maxRecalculateTime, @ParamMessage final AggregateProperty property, final Logger logger) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, sql -> {
            long start = System.currentTimeMillis();
            logger.info(String.format("Recalculate Aggregation started: %s", property.getSID()));
            property.recalculateAggregation(businessLogics, dataSession, sql, LM.baseClass);

            long time = System.currentTimeMillis() - start;
            String message = String.format("Recalculate Aggregation: %s, %sms", property.getSID(), time);
            logger.info(message);
            if (time > maxRecalculateTime)
                messageList.add(message);
        });
    }

    public void recalculateTableClasses(SQLSession session, String tableName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (ImplementTable table : LM.tableFactory.getImplementTables())
            if (tableName.equals(table.getName())) {
                runTableClassesRecalculation(session, table, isolatedTransaction);
            }
    }

    public String checkTableClasses(SQLSession session, String tableName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (ImplementTable table : LM.tableFactory.getImplementTables())
            if (tableName.equals(table.getName())) {
                return DataSession.checkTableClasses(table, session, LM.baseClass, true);
            }
        return null;
    }

    private void runTableClassesRecalculation(SQLSession session, final ImplementTable implementTable, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, sql -> DataSession.recalculateTableClasses(implementTable, sql, LM.baseClass));
    }


    public void recalculateAggregationTableColumn(DataSession dataSession, SQLSession session, String propertyCanonicalName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        AggregateProperty property = businessLogics.getAggregateStoredProperty(propertyCanonicalName);
        if (property != null)
            runAggregationRecalculation(dataSession, session, property, null, isolatedTransaction);
    }

    public <P extends PropertyInterface> void runAggregationRecalculation(final DataSession dataSession, SQLSession session, final AggregateProperty<P> aggregateProperty, PropertyChange<P> where, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        runAggregationRecalculation(dataSession, session, aggregateProperty, where, isolatedTransaction, true);
    }

    public <P extends PropertyInterface> void runAggregationRecalculation(final DataSession dataSession, SQLSession session, final AggregateProperty<P> aggregateProperty, PropertyChange<P> where, boolean isolatedTransaction, boolean recalculateClasses) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, sql -> aggregateProperty.recalculateAggregation(businessLogics, dataSession, sql, LM.baseClass, where, recalculateClasses));
    }

    public void recalculateAggregationWithDependenciesTableColumn(SQLSession session, ExecutionStack stack, String propertyCanonicalName, boolean isolatedTransaction, boolean dependencies) throws SQLException, SQLHandledException {
        try(DataSession dataSession = createRecalculateSession(session)) {
            List<AggregateProperty> properties = getDependentProperties(dataSession, businessLogics.findProperty(propertyCanonicalName).property, new HashSet<>(), dependencies);
            for(AggregateProperty prop : properties) {
                runAggregationRecalculation(dataSession, session, prop, null, isolatedTransaction);
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

        } catch (SQLException | SQLHandledException e) {
            serviceLogger.info(e.getMessage());
        }
        return disableStatsTableColumnSet.toJavaSet();
    }


    private void checkModules(OldDBStructure dbStructure) {
        String droppedModules = "";
        for (String moduleName : dbStructure.modulesList)
            if (businessLogics.getSysModule(moduleName) == null) {
                startLogger.info("Module " + moduleName + " has been dropped");
                droppedModules += moduleName + ", ";
            }
        if (denyDropModules && !droppedModules.isEmpty())
            throw new RuntimeException("Dropped modules: " + droppedModules.substring(0, droppedModules.length() - 2));
    }

    private synchronized void runMigrationScript() {
        if (ignoreMigration) {
            //todo: добавить возможность задавать расположение для migration.script, чтобы можно было запускать разные логики из одного модуля
            return;
        }

        if (!migrationScriptWasRead) {
            try {
                migrationManager = new MigrationManager(namingPolicy);
                InputStream scriptStream = getClass().getResourceAsStream("/migration.script");
                if (scriptStream != null) {
                    ANTLRInputStream stream = new ANTLRInputStream(scriptStream);
                    MigrationScriptLexer lexer = new MigrationScriptLexer(stream);
                    MigrationScriptParser parser = new MigrationScriptParser(new CommonTokenStream(lexer));

                    parser.self = migrationManager;

                    parser.script();
                    migrationScriptWasRead = true;
                }
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
    }

    private void renameColumn(SQLSession sql, OldDBStructure oldData, DBStoredProperty oldProperty, String newName) throws SQLException {
        String oldName = oldProperty.getDBName();
        if (!oldName.equals(newName)) {
            startLogger.info("Renaming column from " + oldName + " to " + newName + " in table " + oldProperty.tableName);
            sql.renameColumn(oldProperty.getTableName(getSyntax()), oldName, newName);
            PropertyField field = oldData.getTable(oldProperty.tableName).findProperty(oldName);
            field.setName(newName);
        }
    }
    
    private void renameMigratingProperties(SQLSession sql, OldDBStructure oldData) throws SQLException {
        Map<String, String> propertyChanges = migrationManager.getStoredPropertyCNChangesAfter(oldData.migrationVersion);
        for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
            boolean found = false;
            String newDBName = getNamingPolicy().transformActionOrPropertyCNToDBName(entry.getValue());
            for (DBStoredProperty oldProperty : oldData.storedProperties) {
                if (entry.getKey().equals(oldProperty.getCanonicalName())) {
                    renameColumn(sql, oldData, oldProperty, newDBName);
                    oldProperty.migrateNames(entry.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) {
                startLogger.warn("Property " + entry.getKey() + " was not found for renaming to " + entry.getValue());
            }
        }
    }
    
    private void renameMigratingTables(SQLSession sql, OldDBStructure oldData) throws SQLException {
        Map<String, String> tableChanges = migrationManager.getTableSIDChangesAfter(oldData.migrationVersion);
        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            if (tableChanges.containsKey(oldProperty.tableName)) {
                oldProperty.tableName = tableChanges.get(oldProperty.tableName);
            }
        }

        for (NamedTable table : oldData.tables.keySet()) {
            String tableDBName = table.getName();
            if (tableChanges.containsKey(tableDBName)) {
                String newDBName = tableChanges.get(tableDBName);
                startLogger.info("Renaming table from " + tableDBName + " to " + newDBName);
                sql.renameTable(table, newDBName);
                table.setName(newDBName);
            }
        }
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
            DBStoredProperty classProp = newData.getProperty(cls.sDataPropID);
            assert classProp != null;
            String tableName = classProp.getTable().getName();
            if (tableNewClassProps.containsKey(tableName)) {
                assert cls.sDataPropID.equals(tableNewClassProps.get(tableName));
            } else {
                tableNewClassProps.put(tableName, cls.sDataPropID);
            }
        }
        
        Map<String, String> nameRenames = new HashMap<>();
        for (DBConcreteClass cls : oldData.concreteClasses) {
            if (!nameRenames.containsKey(cls.sDataPropID)) {
                DBStoredProperty oldClassProp = oldData.getProperty(cls.sDataPropID);
                assert oldClassProp != null;
                String tableName = oldClassProp.tableName;
                if (tableNewClassProps.containsKey(tableName)) {
                    String newName = tableNewClassProps.get(tableName);
                    nameRenames.put(cls.sDataPropID, newName);
                    String newDBName = getNamingPolicy().transformActionOrPropertyCNToDBName(newName);
                    renameColumn(sql, oldData, oldClassProp, newDBName);
                    oldClassProp.migrateNames(newName);
                    cls.sDataPropID = newName;
                }
            } else {
                cls.sDataPropID = nameRenames.get(cls.sDataPropID);
            }
        }
    } 
    
    private void migrateDBNames(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        Map<String, DBStoredProperty> newProperties = new HashMap<>();
        for (DBStoredProperty newProperty : newData.storedProperties) {
            newProperties.put(newProperty.getCanonicalName(), newProperty);
        }

        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            DBStoredProperty newProperty;
            if ((newProperty = newProperties.get(oldProperty.getCanonicalName())) != null) {
                if (!newProperty.getDBName().equals(oldProperty.getDBName())) {
                    renameColumn(sql, oldData, oldProperty, newProperty.getDBName());
                    // переустанавливаем каноническое имя, чтобы получить новый dbName
                    oldProperty.migrateNames(oldProperty.getCanonicalName());
                }
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
                            startLogger.info(String.format("Cannot migrate LOG property to '%s': '%s'", logPropCN, e.getMessage()));
                        }
                    }
                }
            }
        }
    }
    
    // Не разбирается с индексами. Было решено, что сохранять индексы необязательно.
    private void alterDBStructure(OldDBStructure oldData, NewDBStructure newData, SQLSession sql) throws SQLException {
        // Сохраним изменения имен свойств на форме и элементов навигатора для reflectionManager
        finalPropertyDrawNameChanges = migrationManager.getPropertyDrawNameChangesAfter(oldData.migrationVersion);
        finalNavigatorElementNameChanges = migrationManager.getNavigatorCNChangesAfter(oldData.migrationVersion);

        // Обязательно до renameMigratingProperties, потому что в storedPropertyCNChanges добавляются изменения для log-свойств 
        addLogPropertiesToMigration(oldData, newData.migrationVersion);
        
        // Изменяем в старой структуре свойства из скрипта миграции, переименовываем поля в таблицах
        renameMigratingProperties(sql, oldData);
        
        // Переименовываем таблицы из скрипта миграции, переустанавливаем ссылки на таблицы в свойствах
        renameMigratingTables(sql, oldData);

        // Переустановим имена классовым свойствам, если это необходимо. Также при необходимости переименуем поля в таблицах   
        // Иимена полей могут измениться при переименовании таблиц (так как в именах классовых свойств есть имя таблицы) либо при изменении dbNamePolicy
        migrateClassProperties(sql, oldData, newData);        
        
        // При изменении dbNamePolicy необходимо также переименовать поля
        migrateDBNames(sql, oldData, newData);
        
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
    public <Z extends PropertyInterface> void addIndex(ImList<PropertyObjectInterfaceImplement<String>> index, IndexType indexType) {
        PropertyRevImplement<Z, String> propertyImplement = (PropertyRevImplement<Z, String>) findProperty(index);
        if(propertyImplement != null) {
            indexes.put(index, new IndexOptions(propertyImplement.property.getType() instanceof DataClass, indexType, Settings.get().getFilterMatchLanguage()));
            propertyImplement.property.markIndexed(propertyImplement.mapping, index);
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

    public String getBackupFilePath(String dumpFileName) throws IOException, InterruptedException {
        return adapter.getBackupFilePath(dumpFileName);
    }

    public String backupDB(ExecutionContext context, String dumpFileName, int threadCount, List<String> excludeTables) throws IOException, InterruptedException {
        return adapter.backupDB(context, dumpFileName, threadCount, excludeTables);
    }

    public String customRestoreDB(String fileBackup, Set<String> tables) throws IOException {
        return adapter.customRestoreDB(fileBackup, tables);
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

    public void packTables(SQLSession session, ImCol<ImplementTable> tables, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (final ImplementTable table : tables) {
            logger.debug(localize("{logics.info.packing.table}") + " (" + table + ")... ");
            run(session, isolatedTransaction, sql -> sql.packTable(table, OperationOwner.unknown, TableOwner.global));
            logger.debug("Done");
        }
    }

    public static int START_TIL = -1;
    public static int DEBUG_TIL = -1;
    public static int RECALC_TIL = -1;
    
    public static boolean DISABLE_SESSION_TIL = false;
    public static int getSessionTIL() {
        return DISABLE_SESSION_TIL ? -1 : getTIL();
    }
    
    public static int getTIL() {
        return Settings.get().isTrueSerializable() ? Connection.TRANSACTION_SERIALIZABLE : Connection.TRANSACTION_REPEATABLE_READ;
    }
    
    private static Stack<Integer> STACK_TIL = new Stack<>();
    
    public static void pushTIL(Integer TIL) {
        STACK_TIL.push(TIL);
    }
    
    public static Integer popTIL() {
        return STACK_TIL.isEmpty() ? null : STACK_TIL.pop();
    }
    
    public static Integer getCurrentTIL() {
        return STACK_TIL.isEmpty() ? getSessionTIL() : STACK_TIL.peek();
    }
    
    public static String HOSTNAME_COMPUTER;

    public static boolean RECALC_REUPDATE = false;
    public static boolean PROPERTY_REUPDATE = false;

    public void dropColumn(String tableName, String columnName) throws SQLException, SQLHandledException {
        SQLSession sql = getThreadLocalSql();
        sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);
        try {
            sql.dropColumn(tableName, columnName, Settings.get().isStartServerAnyWay());
            ImplementTable table = LM.tableFactory.getImplementTablesMap().get(tableName); // надо упаковать таблицу, если удалили колонку
            if (table != null)
                sql.packTable(table, OperationOwner.unknown, TableOwner.global);
            sql.commitTransaction();
        } catch(SQLException e) {
            sql.rollbackTransaction();
            throw e;
        }
    }

    // может вызываться до инициализации DBManager
    private MigrationVersion getOldMigrationVersion(SQLSession sql) throws IOException, SQLException, SQLHandledException {
        MigrationVersion migrationVersion = new MigrationVersion("0.0");
        
        DataInputStream inputDB;
        StructTable structTable = StructTable.instance;
        RawFileData struct = (RawFileData) sql.readRecord(structTable, MapFact.EMPTY(), structTable.struct, OperationOwner.unknown);
        if (struct != null) {
            inputDB = new DataInputStream(struct.getInputStream());
            //noinspection ResultOfMethodCallIgnored
            inputDB.read();
            migrationVersion = new MigrationVersion(inputDB.readUTF());
        }
        return migrationVersion;
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
            mapKeys = ((Property<PropertyInterface>)property).mapTable.mapKeys.mapKeys(value -> value.ID);
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
    }

    private class DBConcreteClass {
        public String sID;
        public String sDataPropID; // в каком ClassDataProperty хранился

        @Override
        public String toString() {
            return sID + ' ' + sDataPropID;
        }

        public Long ID = null; // только для старых
        public ConcreteCustomClass customClass = null; // только для новых

        private DBConcreteClass(String sID, String sDataPropID, Long ID) {
            this.sID = sID;
            this.sDataPropID = sDataPropID;
            this.ID = ID;
        }

        private DBConcreteClass(ConcreteCustomClass customClass) {
            sID = customClass.getSID();
            sDataPropID = customClass.dataProperty.getCanonicalName();

            this.customClass = customClass;
        }
    }

    private abstract class DBStructure<F> {
        public int version;
        public MigrationVersion migrationVersion;
        public List<String> modulesList = new ArrayList<>();
        public Map<NamedTable, Map<List<F>, IndexOptions>> tables = new HashMap<>();
        public List<DBStoredProperty> storedProperties = new ArrayList<>();
        public Set<DBConcreteClass> concreteClasses = new HashSet<>();

        public void writeConcreteClasses(DataOutputStream outDB) throws IOException { // отдельно от write, так как ID заполняются после fillIDs
            outDB.writeInt(concreteClasses.size());
            for (DBConcreteClass concreteClass : concreteClasses) {
                outDB.writeUTF(concreteClass.sID);
                outDB.writeUTF(concreteClass.sDataPropID);
                outDB.writeLong(concreteClass.ID);
            }
        }

        public NamedTable getTable(String name) {
            for (NamedTable table : tables.keySet()) {
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

    public <P extends PropertyInterface> Map<NamedTable, Map<List<Field>, IndexOptions>> getIndicesMap() {
        Map<NamedTable, Map<List<Field>, IndexOptions>> res = new HashMap<>();
        for (ImplementTable table : LM.tableFactory.getImplementTablesMap().valueIt()) {
            res.put(table, new HashMap<>());
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

            List<Field> tableIndex = new ArrayList<>();

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
                tableIndex.add(field);
            }
            res.get(baseIndexTable).put(tableIndex, index.getValue());
        }
        return res;
    }

    private class NewDBStructure extends DBStructure<Field> {
        
        public NewDBStructure(MigrationVersion migrationVersion) {
            version = 34; // need this for migration
            this.migrationVersion = migrationVersion;

            tables.putAll(getIndicesMap());

            for (Property<?> property : businessLogics.getStoredProperties()) {
                storedProperties.add(new DBStoredProperty(property));
                assert property.isNamed();
            }

            for (ConcreteCustomClass customClass : businessLogics.getConcreteCustomClasses()) {
                concreteClasses.add(new DBConcreteClass(customClass));
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
            for (Map.Entry<NamedTable, Map<List<Field>, IndexOptions>> tableIndices : tables.entrySet()) {
                tableIndices.getKey().serialize(outDB);
                outDB.writeInt(tableIndices.getValue().size());
                for (Map.Entry<List<Field>, IndexOptions> index : tableIndices.getValue().entrySet()) {
                    outDB.writeInt(index.getKey().size());
                    for (Field indexField : index.getKey()) {
                        outDB.writeUTF(indexField.getName());
                    }
                    index.getValue().serialize(outDB);
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

    public static int oldDBStructureVersion = 0;

    private class OldDBStructure extends DBStructure<String> {

        public OldDBStructure(DataInputStream inputDB) throws IOException {
            migrationVersion = new MigrationVersion("0.0");
            if (inputDB == null) {
                version = -2;
            } else {
                version = inputDB.read() - 'v';
                oldDBStructureVersion = version;
                migrationVersion = new MigrationVersion(inputDB.readUTF());

                int modulesCount = inputDB.readInt();
                if (modulesCount > 0) {
                    for (int i = 0; i < modulesCount; i++)
                        modulesList.add(inputDB.readUTF());
                }

                for (int i = inputDB.readInt(); i > 0; i--) {
                    SerializedTable prevTable = new SerializedTable(inputDB, LM.baseClass);
                    Map<List<String>, IndexOptions> indices = new HashMap<>();
                    for (int j = inputDB.readInt(); j > 0; j--) {
                        List<String> index = new ArrayList<>();
                        for (int k = inputDB.readInt(); k > 0; k--) {
                            index.add(inputDB.readUTF());
                        }
                        if (version >= 32) {
                            indices.put(index, IndexOptions.deserialize(inputDB));
                        } else {
                            indices.put(index, new IndexOptions(inputDB.readBoolean()));
                        }

                    }
                    tables.put(prevTable, indices);
                }

                int prevStoredNum = inputDB.readInt();
                for (int i = 0; i < prevStoredNum; i++) {
                    String canonicalName = inputDB.readUTF();
                    String sID = inputDB.readUTF();
                    boolean isDataProperty = inputDB.readBoolean();
                    
                    String tableName = inputDB.readUTF();
                    Table prevTable = getTable(tableName);
                    MExclMap<Integer, KeyField> mMapKeys = MapFact.mExclMap(prevTable.getTableKeys().size());
                    for (int j = 0; j < prevTable.getTableKeys().size(); j++) {
                        mMapKeys.exclAdd(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));
                    }
                    storedProperties.add(new DBStoredProperty(canonicalName, sID, isDataProperty, tableName, mMapKeys.immutable()));
                }

                int prevConcreteNum = inputDB.readInt();
                for(int i = 0; i < prevConcreteNum; i++)
                    concreteClasses.add(new DBConcreteClass(inputDB.readUTF(), inputDB.readUTF(), inputDB.readLong()));
            }
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
