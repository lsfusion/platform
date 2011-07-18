package platform.server.data;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.MutableObject;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseInterface;
import platform.server.data.type.Reader;
import platform.server.data.type.Type;
import platform.server.data.type.TypeObject;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;

import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.*;

public class SQLSession extends MutableObject {
    private final static Logger logger = Logger.getLogger(SQLSession.class);

    public SQLSyntax syntax;

    private ConnectionPool connectionPool;

    private Connection getConnection() throws SQLException {
        return privateConnection !=null ? privateConnection : connectionPool.getCommon(this);
    }

    private void returnConnection(Connection connection) throws SQLException {
        if(privateConnection !=null)
            assert privateConnection == connection;
        else
            connectionPool.returnCommon(this, connection);
    }

    private Connection privateConnection = null;

    public final static String userParam = "adsadaweewuser";
    public final static String isServerRestartingParam = "sdfisserverrestartingpdfdf";
    public final static String sessionParam = "dsfreerewrewrsf";
    public final static String computerParam = "fjruwidskldsor";

    public SQLSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connectionPool = adapter;
    }

    private void needPrivate() throws SQLException { // получает unique connection
        if(privateConnection ==null)
            privateConnection = connectionPool.getPrivate(this);
    }

    private void tryCommon() throws SQLException { // пытается вернуться к
        removeUnusedTemporaryTables();
        if(inTransaction == 0 && getSQLTemporaryPool().isEmpty(sessionTablesMap)) { // вернемся к commonConnection'у
            connectionPool.returnPrivate(this, privateConnection);
            privateConnection = null;
        }
    }

    private int inTransaction = 0; // счетчик для по сути распределенных транзакций

    public boolean isInTransaction() {
        return inTransaction > 0;
    }

    public static void setACID(Connection connection, boolean ACID) throws SQLException {
        connection.setAutoCommit(!ACID);
        connection.setReadOnly(!ACID);

        Statement statement = connection.createStatement();
        try {
            statement.execute("SET SESSION synchronous_commit TO " + (ACID ? "DEFAULT" : "OFF"));
            statement.execute("SET SESSION commit_delay TO " + (ACID ? "DEFAULT" : "100000"));
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();
        }
    }

    public void startTransaction() throws SQLException {
        needPrivate();

        if(inTransaction++ == 0)
            setACID(privateConnection, true);
    }

    private void endTransaction() throws SQLException {
        if(--inTransaction == 0)
            setACID(privateConnection, false);

        tryCommon();
    }

    public void rollbackTransaction() throws SQLException {
        privateConnection.rollback();
        endTransaction();
    }

    public void commitTransaction() throws SQLException {
        privateConnection.commit();
        endTransaction();
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table) throws SQLException {
        Connection connection = getConnection();

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, table.name, new String[]{"TABLE"});
        if (!tables.next()) {
            createTable(table.name, table.keys);
            for (PropertyField property : table.properties)
                addColumn(table.name, property);
        }

        returnConnection(connection);
    }

    public void addExtraIndices(String table, List<KeyField> keys) throws SQLException {
        List<String> keyStrings = new ArrayList<String>();
        for(KeyField key : keys)
            keyStrings.add(key.name);
        for(int i=1;i<keys.size();i++)
            addIndex(table, keyStrings.subList(i, keys.size()));
    }

    private String getConstraintName(String table) {
        return "PK_" + table;
    }

    private String getConstraintDeclare(String table, List<KeyField> keys) {
        String keyString = "";
        for (KeyField key : keys)
            keyString = (keyString.length() == 0 ? "" : keyString + ',') + key.name;
        return "CONSTRAINT " + getConstraintName(table) + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";
    }

    public void createTable(String table, List<KeyField> keys) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.table.creation") + " " + table + "... ");
        if (keys.size() == 0)
            keys = Collections.singletonList(KeyField.dumb);
        String createString = "";
        for (KeyField key : keys)
            createString = (createString.length() == 0 ? "" : createString + ',') + key.getDeclare(syntax);
        createString = createString + "," + getConstraintDeclare(table, keys);

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        executeDDL("CREATE TABLE " + table + " (" + createString + ")", false);
        addExtraIndices(table, keys);
        logger.info(" Done");
    }

    public void dropTable(String table) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.table.deletion") + " " + table + "... ");
        executeDDL("DROP TABLE " + table, false);
        logger.info(" Done");
    }

    static String getIndexName(String table, Collection<String> fields) {
        String name = table + "_idx";
        for (String indexField : fields)
            name = name + "_" + indexField;
        return name;
    }

    public void addIndex(String table, List<String> fields) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.index.creation") + " " + getIndexName(table, fields) + "... ");
        String columns = "";
        for (String indexField : fields)
            columns = (columns.length() == 0 ? "" : columns + ",") + indexField;

        executeDDL("CREATE INDEX " + getIndexName(table, fields) + " ON " + table + " (" + columns + ")", false);
        logger.info(" Done");
    }

    public void dropIndex(String table, Collection<String> fields) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.index.deletion") + " " + getIndexName(table, fields) + "... ");
        executeDDL("DROP INDEX " + getIndexName(table, fields), false);
        logger.info(" Done");
    }

/*    public void addKeyColumns(String table, Map<KeyField, Object> fields, List<KeyField> keys) throws SQLException {
        if(fields.isEmpty())
            return;

        logger.info("Идет добавление ключа " + table + "." + fields + "... ");

        String constraintName = getConstraintName(table);
        String tableName = syntax.getSessionTableName(table);
        String addCommand = ""; String dropDefaultCommand = "";
        for(Map.Entry<KeyField, Object> field : fields.entrySet()) {
            addCommand = addCommand + "ADD COLUMN " + field.getKey().getDeclare(syntax) + " DEFAULT " + field.getKey().type.getString(field.getValue(), syntax) + ",";
            dropDefaultCommand = (dropDefaultCommand.length()==0?"":dropDefaultCommand + ",") + " ALTER COLUMN " + field.getKey().name + " DROP DEFAULT";
        }

        execute("ALTER TABLE " + tableName + " " + addCommand + " ADD " + getConstraintDeclare(table, keys) +
                (keys.size()==fields.size()?"":", DROP CONSTRAINT " + constraintName));
        execute("ALTER TABLE " + tableName + " " + dropDefaultCommand);

        logger.info(" Done");
    }

    public void addTemporaryColumn(String table, PropertyField field) throws SQLException {
        addColumn(table, field);
//        execute("CREATE INDEX " + "idx_" + table + "_" + field.name + " ON " + table + " (" + field.name + ")"); //COLUMN
    }*/

    public void addColumn(String table, PropertyField field) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.column.adding") + " " + table + "." + field.name + "... ");
        executeDDL("ALTER TABLE " + table + " ADD " + field.getDeclare(syntax), false); //COLUMN
        logger.info(" Done");
    }

    public void dropColumn(String table, String field) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.column.deletion") + " " + table + "." + field + "... ");
        executeDDL("ALTER TABLE " + table + " DROP COLUMN " + field, false);
        logger.info(" Done");
    }

    public void modifyColumn(String table, PropertyField field, Type oldType) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.column.type.changing") + " " + table + "." + field.name + "... ");
        executeDDL("ALTER TABLE " + table + " ALTER COLUMN " + field.name + " TYPE " +
                field.type.getDB(syntax) + " " + syntax.typeConvertSuffix(oldType, field.type, field.name), false);
        logger.info(" Done");
    }

    public void packTable(Table table) throws SQLException {
        logger.info(ServerResourceBundle.getString("data.table.packing")+" " + table + "... ");
        String dropWhere = "";
        for (PropertyField property : table.properties)
            dropWhere = (dropWhere.length() == 0 ? "" : dropWhere + " AND ") + property.name + " IS NULL";
        executeDML("DELETE FROM " + table.getName(syntax) + (dropWhere.length() == 0 ? "" : " WHERE " + dropWhere));
        logger.info(" Done");
    }

    private SQLTemporaryPool temporaryPool = new SQLTemporaryPool();
    private SQLTemporaryPool getSQLTemporaryPool() { // в зависимости от политики или локальный пул (для сессии) или глобальный пул
        return temporaryPool;
    }

    private final Map<String, WeakReference<Object>> sessionTablesMap = new HashMap<String, WeakReference<Object>>();
    private int sessionCounter = 0;

    public String getTemporaryTable(List<KeyField> keys, Set<PropertyField> properties, FillTemporaryTable fill, Integer count, Result<Integer> actual, Object owner) throws SQLException {
        needPrivate();

        removeUnusedTemporaryTables();

        String table = getSQLTemporaryPool().getTable(this, keys, properties, fill, count, actual, sessionTablesMap);
        sessionTablesMap.put(table, new WeakReference<Object>(owner));

        return table;
    }

    private void removeUnusedTemporaryTables() throws SQLException {
        synchronized (sessionTablesMap) {
            for (Iterator<Map.Entry<String, WeakReference<Object>>> iterator = sessionTablesMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, WeakReference<Object>> entry = iterator.next();
                if (entry.getValue().get() == null) {
//                    dropTemporaryTableFromDB(entry.getKey());
                    iterator.remove();
                }
            }
        }
    }

    public void returnTemporaryTable(SessionTable table, Object owner) throws SQLException {
        synchronized (sessionTablesMap) {
            assert sessionTablesMap.containsKey(table.name);
            WeakReference<Object> removed = sessionTablesMap.remove(table.name);
            assert removed.get()==owner;

//            dropTemporaryTableFromDB(table.name);
        }

        tryCommon();
    }

    // напрямую не используется, только через Pool

    private void dropTemporaryTableFromDB(String tableName) throws SQLException {
        executeDDL(syntax.getDropSessionTable(tableName), true);
    }

    public void createTemporaryTable(String name, List<KeyField> keys, Collection<PropertyField> properties) throws SQLException {
        String createString = "";
        if(keys.size()==0)
            keys = Collections.singletonList(KeyField.dumb);
        for (KeyField key : keys)
            createString = (createString.length() == 0 ? "" : createString + ',') + key.getDeclare(syntax);
        for (PropertyField prop : properties)
            createString = (createString.length() == 0 ? "" : createString + ',') + prop.getDeclare(syntax);
        createString = createString + "," + getConstraintDeclare(name, keys);
        executeDDL(syntax.getCreateSessionTable(name, createString), true);
    }

    public void analyzeSessionTable(String table) throws SQLException {
//        if(inTransaction)
        executeDDL("ANALYZE " + table, true);
//        else
//            execute("VACUUM ANALYZE " + table);
    }

    private void executeDDL(String DDL, boolean temporary) throws SQLException {
        Connection connection = getConnection();

        logger.info(DDL);

        if(temporary && inTransaction == 0) // если temporary таблица то для DDL надо выключать read-only
            connection.setReadOnly(false);

        Statement statement = connection.createStatement();
        try {
            statement.execute(DDL);
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();

            if(temporary && inTransaction == 0)
                connection.setReadOnly(true);

            returnConnection(connection);
        }
    }

    private int executeDML(SQLExecute execute) throws SQLException {
        return executeDML(execute.command, execute.params);
    }

    private boolean explainAnalyzeMode = false;
    private void executeExplain(PreparedStatement statement) throws SQLException {
        ResultSet result = statement.executeQuery();
        try {
            while(result.next()) {
                System.out.println(result.getObject("QUERY PLAN"));
            }
        } finally {
            result.close();
        }
    }

    private int executeDML(String command, Map<String, ParseInterface> paramObjects) throws SQLException {
        Connection connection = getConnection();

        PreparedStatement statement = getStatement((explainAnalyzeMode?"EXPLAIN ANALYZE ":"") + command, paramObjects, connection, syntax);

        int result;
        try {
            if(explainAnalyzeMode) {
                executeExplain(statement);
                result = 100;
            } else
                result = statement.executeUpdate();
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }

        return result;
    }

    private void executeDML(String executeString) throws SQLException {
        Connection connection = getConnection();

        logger.info(executeString);

        Statement statement = connection.createStatement();
        try {
            statement.executeUpdate(executeString);
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }
    }

    public <K,V> OrderedMap<Map<K, Object>, Map<V, Object>> executeSelect(String select, Map<String, ParseInterface> paramObjects, Map<K, String> keyNames, Map<K, ? extends Reader> keyReaders, Map<V, String> propertyNames, Map<V, ? extends Reader> propertyReaders) throws SQLException {
        Connection connection = getConnection();

        logger.info(select);

        if(explainAnalyzeMode) {
            executeExplain(getStatement("EXPLAIN ANALYZE " + select, paramObjects, connection, syntax));
        }

        PreparedStatement statement = getStatement(select, paramObjects, connection, syntax);
        OrderedMap<Map<K,Object>,Map<V,Object>> execResult = new OrderedMap<Map<K, Object>, Map<V, Object>>();
        try {
            ResultSet result = statement.executeQuery();
            try {
                while(result.next()) {
                    Map<K,Object> rowKeys = new HashMap<K, Object>();
                    for(Map.Entry<K,String> key : keyNames.entrySet())
                        rowKeys.put(key.getKey(), keyReaders.get(key.getKey()).read(result.getObject(key.getValue())));
                    Map<V,Object> rowProperties = new HashMap<V, Object>();
                    for(Map.Entry<V,String> property : propertyNames.entrySet())
                        rowProperties.put(property.getKey(),
                                propertyReaders.get(property.getKey()).read(result.getObject(property.getValue())));
                    Map<V, Object> prev = execResult.put(rowKeys, rowProperties);
                    assert prev==null;
                }
            } finally {
                result.close();
            }
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }

        return execResult;
    }

    public void insertBatchRecords(String table, List<KeyField> keys, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) throws SQLException {
        Connection connection = getConnection();

        List<PropertyField> properties = new ArrayList<PropertyField>(rows.values().iterator().next().keySet());

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for (KeyField key : keys) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + "?";
        }

        // пробежим по Fields'ам
        for (PropertyField prop : properties) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + prop.name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + "?";
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        PreparedStatement statement = connection.prepareStatement("INSERT INTO " + syntax.getSessionTableName(table) + " (" + insertString + ") VALUES (" + valueString + ")");

        try {
            for(Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet()) {
                int p=1;
                for(KeyField key : keys)
                    new TypeObject(row.getKey().get(key)).writeParam(statement, p++, syntax);
                for(PropertyField property : properties) {
                    ObjectValue propValue = row.getValue().get(property);
                    if(propValue instanceof NullValue)
                        statement.setNull(p++, property.type.getSQL(syntax));
                    else
                        new TypeObject((DataObject) propValue).writeParam(statement, p++, syntax);
                }
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }
    }

    private void insertParamRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {
        String insertString = "";
        String valueString = "";

        int paramNum = 0;
        Map<String, ParseInterface> params = new HashMap<String, ParseInterface>();

        // пробежим по KeyFields'ам
        for (KeyField key : table.keys) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.name;
            DataObject keyValue = keyFields.get(key);
            if (keyValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.put(prm, new TypeObject(keyValue));
            }
        }

        for (Map.Entry<PropertyField, ObjectValue> fieldValue : propFields.entrySet()) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + fieldValue.getKey().name;
            if (fieldValue.getValue().isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + fieldValue.getValue().getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.put(prm, new TypeObject((DataObject) fieldValue.getValue()));
            }
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", params);
    }

    public void insertRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {

        boolean needParam = false;

        for (Map.Entry<KeyField, DataObject> keyField : keyFields.entrySet())
            if (!keyField.getKey().type.isSafeString(keyField.getValue())) {
                needParam = true;
            }

        for (Map.Entry<PropertyField, ObjectValue> fieldValue : propFields.entrySet())
            if (!fieldValue.getKey().type.isSafeString(fieldValue.getValue())) {
                needParam = true;
            }

        if (needParam) {
            insertParamRecord(table, keyFields, propFields);
            return;
        }

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for (KeyField key : table.keys) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyFields.get(key).getString(syntax);
        }

        // пробежим по Fields'ам
        for (PropertyField prop : propFields.keySet()) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + prop.name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + propFields.get(prop).getString(syntax);
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")");
    }

    public boolean isRecord(Table table, Map<KeyField, DataObject> keyFields) throws SQLException {

        // по сути пустое кол-во ключей
        Query<KeyField, String> query = new Query<KeyField, String>(keyFields.keySet());
        query.putKeyWhere(keyFields);

        // сначала закинем KeyField'ы и прогоним Select
        query.and(table.joinAnd(query.mapKeys).getWhere());

        return query.execute(this).size() > 0;
    }

    public void ensureRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {
        if (!isRecord(table, keyFields))
            insertRecord(table, keyFields, propFields);
    }

    public boolean insertRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update) throws SQLException {
        if(update && isRecord(table, keyFields)) {
            if(!propFields.isEmpty()) {
                Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(table);
                updateQuery.putKeyWhere(keyFields);
                updateQuery.properties.putAll(ObjectValue.getMapExprs(propFields));

                // есть запись нужно Update лупить
                updateRecords(new ModifyQuery(table, updateQuery));
            }
            return false;
        } else {
            insertRecord(table, keyFields, propFields);
            return true;
        }
    }

    public Object readRecord(Table table, Map<KeyField, DataObject> keyFields, PropertyField field) throws SQLException {
        // по сути пустое кол-во ключей
        Query<KeyField, String> query = new Query<KeyField, String>(keyFields.keySet());

        // сначала закинем KeyField'ы и прогоним Select
        Expr fieldExpr = table.joinAnd(query.mapKeys).getExpr(field);
        query.putKeyWhere(keyFields);
        query.properties.put("result", fieldExpr);
        query.and(fieldExpr.getWhere());
        OrderedMap<Map<KeyField, Object>, Map<String, Object>> result = query.execute(this);
        if (result.size() > 0)
            return result.singleValue().get("result");
        else
            return null;
    }

    public void truncate(String table) throws SQLException {
//        executeDML("TRUNCATE " + syntax.getSessionTableName(table));
        executeDML("DELETE FROM " + syntax.getSessionTableName(table));
    }

    public void deleteKeyRecords(Table table, Map<KeyField, ?> keys) throws SQLException {
        String deleteWhere = "";
        for (Map.Entry<KeyField, ?> deleteKey : keys.entrySet())
            deleteWhere = (deleteWhere.length() == 0 ? "" : deleteWhere + " AND ") + deleteKey.getKey().name + "=" + deleteKey.getValue();

        executeDML("DELETE FROM " + table.getName(syntax) + (deleteWhere.length() == 0 ? "" : " WHERE " + deleteWhere));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getUpdate(syntax));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getInsertSelect(syntax));
    }
    public int insertSessionSelect(String name, Query<KeyField, PropertyField> query, QueryEnvironment env) throws SQLException {
        return executeDML(ModifyQuery.getInsertSelect(syntax.getSessionTableName(name), query, env, syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public int modifyRecords(ModifyQuery modify) throws SQLException {
        if (modify.isEmpty()) return 0;
        if (modify.table.isSingle()) {// потому как запросом никак не сделаешь, просто вкинем одну пустую запись
            if (!isRecord(modify.table, new HashMap<KeyField, DataObject>()))
                insertSelect(modify);
        } else
            executeDML(modify.getInsertLeftKeys(syntax));
        return updateRecords(modify);
    }

    public void close() throws SQLException {
        if(privateConnection !=null)
            privateConnection.close();
    }

    private static PreparedStatement getStatement(String command, Map<String, ParseInterface> paramObjects, Connection connection, SQLSyntax syntax) throws SQLException {

        char[][] params = new char[paramObjects.size()][];
        ParseInterface[] values = new ParseInterface[params.length];
        int paramNum = 0;
        for (Map.Entry<String, ParseInterface> param : paramObjects.entrySet()) {
            params[paramNum] = param.getKey().toCharArray();
            values[paramNum++] = param.getValue();
        }

        // те которые isString сразу транслируем
        List<ParseInterface> preparedParams = new ArrayList<ParseInterface>();
        char[] toparse = command.toCharArray();
        String parsedString = "";
        char[] parsed = new char[toparse.length + params.length * 100];
        int num = 0;
        for (int i = 0; i < toparse.length;) {
            int charParsed = 0;
            for (int p = 0; p < params.length; p++) {
                if (BaseUtils.startsWith(toparse, i, params[p])) { // нашли
                    if (values[p].isSafeString()) { // если можно вручную пропарсить парсим
                        parsedString = parsedString + new String(parsed, 0, num) + values[p].getString(syntax);
                        parsed = new char[toparse.length - i + (params.length - p) * 100];
                        num = 0;
                    } else {
                        parsed[num++] = '?';
                        if (!values[p].isSafeType()) {
                            String castString = "::" + values[p].getDBType(syntax);
                            System.arraycopy(castString.toCharArray(), 0, parsed, num, castString.length());
                            num += castString.length();
                        }
                        preparedParams.add(values[p]);
                    }
                    charParsed = params[p].length;
                    break;
                }
            }
            if (charParsed == 0) {
                parsed[num++] = toparse[i];
                charParsed = 1;
            }
            i = i + charParsed;
        }
        parsedString = parsedString + new String(parsed, 0, num);

        logger.info(parsedString);

        PreparedStatement statement = connection.prepareStatement(parsedString);
        paramNum = 1;
        for (ParseInterface param : preparedParams)
            param.writeParam(statement, paramNum++, syntax);

        return statement;
    }

    // вспомогательные методы
    public static String stringExpr(Map<String, String> keySelect, Map<String, String> propertySelect) {
        String expressionString = "";
        for (Map.Entry<String, String> key : keySelect.entrySet())
            expressionString = (expressionString.length() == 0 ? "" : expressionString + ",") + key.getValue() + " AS " + key.getKey();
        for (Map.Entry<String, String> property : propertySelect.entrySet())
            expressionString = (expressionString.length() == 0 ? "" : expressionString + ",") + property.getValue() + " AS " + property.getKey();
        if (expressionString.length() == 0)
            expressionString = "0";
        return expressionString;
    }

    public static <T> OrderedMap<String, String> mapNames(Map<T, String> exprs, Map<T, String> names, List<T> order) {
        OrderedMap<String, String> result = new OrderedMap<String, String>();
        if (order.isEmpty())
            for (Map.Entry<T, String> name : names.entrySet()) {
                result.put(name.getValue(), exprs.get(name.getKey()));
                order.add(name.getKey());
            }
        else // для union all
            for (T expr : order)
                result.put(names.get(expr), exprs.get(expr));
        return result;
    }
}
