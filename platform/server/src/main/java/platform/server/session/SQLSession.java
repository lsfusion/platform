package platform.server.session;

import platform.base.BaseUtils;
import platform.server.data.*;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.TypeObject;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.*;
import java.util.*;

public class SQLSession {

    public SQLSyntax syntax;
    
    private Connection connection;

    private boolean inTransaction = false;

    public SQLSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connection = adapter.startConnection();
    }

    public void startTransaction() throws SQLException {
        inTransaction = true;

        if(!syntax.noAutoCommit())
            execute(syntax.startTransaction());
    }

    public void rollbackTransaction() throws SQLException {
        execute(syntax.rollbackTransaction());

        inTransaction = false;
    }

    public void commitTransaction() throws SQLException {
        execute(syntax.commitTransaction());

        inTransaction = false;
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table) throws SQLException {

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, table.name, new String[]{"TABLE"});
        if(!tables.next()) {
            createTable(table.name,table.keys);
            for(PropertyField property : table.properties)
                addColumn(table.name,property);
        }
    }

    public void createTable(String table,Collection<KeyField> keys) throws SQLException {
        System.out.print("Идет создание таблицы "+table+"... ");
        String createString = "";
        String keyString = "";
        for(KeyField key : keys) {
            createString = (createString.length()==0?"": createString +',') + key.getDeclare(syntax);
            keyString = (keyString.length()==0?"":keyString+',') + key.name;
        }
        if(createString.length()==0)
            createString = "dumb bit";
        else
            createString = createString + ",CONSTRAINT PK_" + table + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        execute("CREATE TABLE "+ table +" ("+ createString +")");
        System.out.println(" Done");
    }
    public void dropTable(String table) throws SQLException {
        System.out.print("Идет удаление таблицы "+table+"... ");
        execute("DROP TABLE "+ table);
        System.out.println(" Done");
    }
    static String getIndexName(String table,Collection<String> fields) {
        String name = table + "_idx";
        for(String indexField : fields)
            name = name + "_" + indexField;
        return name;
    }
    public void addIndex(String table,Collection<String> fields) throws SQLException {
        System.out.print("Идет создание индекса "+getIndexName(table, fields)+"... ");
        String columns = "";
        for(String indexField : fields)
            columns = (columns.length()==0?"":columns+",") + indexField;

        execute("CREATE INDEX " + getIndexName(table, fields) + " ON " + table + " (" + columns + ")");
        System.out.println(" Done");
    }
    public void dropIndex(String table,Collection<String> fields) throws SQLException {
        System.out.print("Идет удаление индекса "+getIndexName(table, fields)+"... ");
        execute("DROP INDEX " + getIndexName(table, fields));
        System.out.println(" Done");
    }
    public void addColumn(String table,PropertyField field) throws SQLException {
        System.out.print("Идет добавление колонки "+table+"."+field.name+"... ");
        execute("ALTER TABLE " + table + " ADD COLUMN " + field.getDeclare(syntax));
        System.out.println(" Done");
    }
    public void dropColumn(String table,String field) throws SQLException {
        System.out.print("Идет удаление колонки "+table+"."+field+"... ");
        execute("ALTER TABLE " + table + " DROP COLUMN " + field);
        System.out.println(" Done");
    }
    public void modifyColumn(String table,PropertyField field) throws SQLException {
        System.out.print("Идет изменение типа колонки "+table+"."+field.name+"... ");
        execute("ALTER TABLE " + table + " ALTER COLUMN " + field.name + " TYPE " + field.type.getDB(syntax));
        System.out.println(" Done");
    }

    public void packTable(Table table) throws SQLException {
        System.out.print("Идет упаковка таблицы "+table+"... ");
        String dropWhere = "";
        for(PropertyField property : table.properties)
            dropWhere = (dropWhere.length()==0?"":dropWhere+" AND ") + property.name + " IS NULL";
        execute("DELETE FROM "+ table.getName(syntax) + (dropWhere.length()==0?"":" WHERE "+dropWhere));
        System.out.println(" Done");
    }

    public void createTable(Table table) throws SQLException {
        String createString = "";
        String keyString = "";
        for(KeyField key : table.keys) {
            createString = (createString.length()==0?"": createString +',') + key.getDeclare(syntax);
            keyString = (keyString.length()==0?"":keyString+',') + key.name;
        }
        for(PropertyField prop : table.properties)
            createString = (createString.length()==0?"":createString +',') + prop.getDeclare(syntax);
        if(createString.length()==0)
            createString = "dumb bit";
        else
            createString = createString + ",CONSTRAINT PK_" + table.name + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";

        execute("CREATE TABLE "+ table.name +" ("+ createString +")");
    }

    public void createTemporaryTable(SessionTable<?> table) throws SQLException {
        String createString = "";
        String keyString = "";
        for(KeyField key : table.keys) {
            createString = (createString.length()==0?"":createString+',') + key.getDeclare(syntax);
            keyString = (keyString.length()==0?"":keyString+',') + key.name;
        }
        for(PropertyField prop : table.properties)
            createString = createString+',' + prop.getDeclare(syntax);

//        try { execute("DROP TABLE "+table.name +" CASCADE CONSTRAINTS"); } catch (SQLException e) {  e.getErrorCode(); }

        // "CONSTRAINT PK_S_" + ID +"_T_" + table.name + "
        execute(syntax.getCreateSessionTable(table.name,createString,"PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")"));
    }

    public void dropTemporaryTable(SessionTable table) throws SQLException {
        execute(syntax.getDropSessionTable(table.name));
    }

    private void execute(String executeString) throws SQLException {
        executeStatement(connection.prepareStatement(executeString));
    }

    private void executeStatement(PreparedStatement statement) throws SQLException {
//        System.out.println(statement);
        try {
            statement.execute();
        } catch(SQLException e) {
            System.out.println(statement.toString());
            throw e;
        } finally {
            statement.close();
        }

        ensureTransaction();
    }

    private void ensureTransaction() throws SQLException {
        if(!inTransaction && syntax.noAutoCommit()) {
            Statement statement = connection.createStatement();
            statement.execute(syntax.commitTransaction()+ syntax.getCommandEnd());
            statement.close();
        }
    }

    private void insertParamRecord(Table table,Map<KeyField,DataObject> keyFields,Map<PropertyField,ObjectValue> propFields) throws SQLException {
        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for(KeyField key : table.keys) {
            insertString = (insertString.length()==0?"":insertString+',') + key.name;
            valueString = (valueString.length()==0?"":valueString+',') + keyFields.get(key).object;
        }

        int paramNum = 0;
        Map<String,TypeObject> params = new HashMap<String, TypeObject>();
        for(Map.Entry<PropertyField,ObjectValue> fieldValue : propFields.entrySet()) {
            insertString = (insertString.length()==0?"":insertString+',') + fieldValue.getKey().name;
            if(fieldValue.getValue().isString(syntax))
                valueString = (valueString.length()==0?"":valueString+',') + fieldValue.getValue().getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length()==0?"":valueString+',') + prm;
                params.put(prm, new TypeObject((DataObject)fieldValue.getValue()));
            }
        }

        executeStatement(getStatement("INSERT INTO "+table.getName(syntax)+" ("+insertString+") VALUES ("+valueString+")", params));
    }

    public void insertRecord(Table table,Map<KeyField,DataObject> keyFields,Map<PropertyField,ObjectValue> propFields) throws SQLException {

        for(Map.Entry<PropertyField,ObjectValue> fieldValue : propFields.entrySet())
            if(!fieldValue.getKey().type.isSafeString(fieldValue.getValue())) {
                insertParamRecord(table, keyFields, propFields);
                return;
            }

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for(KeyField key : table.keys) {
            insertString = (insertString.length()==0?"":insertString+',') + key.name;
            valueString = (valueString.length()==0?"":valueString+',') + keyFields.get(key).object;
        }

        // пробежим по Fields'ам
        for(PropertyField prop : propFields.keySet()) {
            insertString = (insertString.length()==0?"":insertString+',') + prop.name;
            valueString = (valueString.length()==0?"":valueString+',') + propFields.get(prop).getString(syntax);
        }

        execute("INSERT INTO "+table.getName(syntax)+" ("+insertString+") VALUES ("+valueString+")");
    }

    public boolean isRecord(Table table,Map<KeyField,DataObject> keyFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> isRecQuery = new JoinQuery<Object,String>(new HashMap<Object, KeyExpr>());

        Map<KeyField, AndExpr> keyExprs = new HashMap<KeyField, AndExpr>();
        for(KeyField key : table.keys)
            keyExprs.put(key,keyFields.get(key).getExpr());

        // сначала закинем KeyField'ы и прогоним Select
        isRecQuery.and(table.joinAnd(keyExprs).getWhere());

        return isRecQuery.executeSelect(this).size()>0;
    }

    public void ensureRecord(Table table,Map<KeyField,DataObject> keyFields,Map<PropertyField,ObjectValue> propFields) throws SQLException {
        if(!isRecord(table, keyFields))
            insertRecord(table,keyFields,propFields);
    }

    public void updateInsertRecord(Table table,Map<KeyField, DataObject> keyFields,Map<PropertyField,ObjectValue> propFields) throws SQLException {

        if(isRecord(table, keyFields)) {
            JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(table);
            updateQuery.putKeyWhere(keyFields);
            for(Map.Entry<PropertyField,ObjectValue> mapProp : propFields.entrySet())
                updateQuery.properties.put(mapProp.getKey(), mapProp.getValue().getExpr());

            // есть запись нужно Update лупить
            updateRecords(new ModifyQuery(table,updateQuery));
        } else
            // делаем Insert
            insertRecord(table,keyFields,propFields);
    }

    public Object readRecord(Table table,Map<KeyField,DataObject> keyFields,PropertyField field) throws SQLException {
        // по сути пустое кол-во ключей
        JoinQuery<Object,String> getQuery = new JoinQuery<Object,String>(new HashMap<Object,KeyExpr>());

        Map<KeyField,AndExpr> keyExprs = new HashMap<KeyField,AndExpr>();
        for(KeyField key : table.keys)
            keyExprs.put(key,keyFields.get(key).getExpr());

        // сначала закинем KeyField'ы и прогоним Select
        SourceExpr fieldExpr = table.joinAnd(keyExprs).getExpr(field);
        getQuery.properties.put("result",fieldExpr);
        getQuery.and(fieldExpr.getWhere());
        LinkedHashMap<Map<Object, Object>, Map<String, Object>> result = getQuery.executeSelect(this);
        if(result.size()>0)
            return BaseUtils.singleValue(result).get("result");
        else
            return null;
    }

    public void deleteKeyRecords(Table table,Map<KeyField,Integer> keys) throws SQLException {
        String deleteWhere = "";
        for(Map.Entry<KeyField,Integer> deleteKey : keys.entrySet())
            deleteWhere = (deleteWhere.length()==0?"":deleteWhere+" AND ") + deleteKey.getKey().name + "=" + deleteKey.getValue();

        execute("DELETE FROM "+ table.getName(syntax)+(deleteWhere.length()==0?"":" WHERE "+deleteWhere));
    }

    public void updateRecords(ModifyQuery modify) throws SQLException {
        SQLExecute update = modify.getUpdate(syntax);
        executeStatement(getStatement(update.command, update.params));
    }

    public void insertSelect(ModifyQuery modify) throws SQLException {
        SQLExecute insertSelect = modify.getInsertSelect(syntax);
        executeStatement(getStatement(insertSelect.command, insertSelect.params));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public void modifyRecords(ModifyQuery modify) throws SQLException {
        SQLExecute leftKeys = modify.getInsertLeftKeys(syntax);
        executeStatement(getStatement(leftKeys.command, leftKeys.params));
        updateRecords(modify);
    }

    public void close() throws SQLException {
        connection.close();
    }

    public PreparedStatement getStatement(String command, Map<String, TypeObject> paramObjects) throws SQLException {

        char[][] params = new char[paramObjects.size()][];
        TypeObject[] values = new TypeObject[params.length];
        int paramNum = 0;
        for(Map.Entry<String,TypeObject> param : paramObjects.entrySet()) {
            params[paramNum] = param.getKey().toCharArray();
            values[paramNum++] = param.getValue();
        }

        // те которые isString сразу транслируем
        List<TypeObject> preparedParams = new ArrayList<TypeObject>();
        char[] toparse = command.toCharArray();
        String parsedString = "";
        char[] parsed = new char[toparse.length]; int num=0;
        for(int i=0;i<toparse.length;) {
            int charParsed = 0;
            for(int p=0;p<params.length;p++) {
                if(BaseUtils.startsWith(toparse,i,params[p])) { // нашли
                    if(values[p].isString()) { // если можно вручную пропарсить парсим
                        parsedString = parsedString + new String(parsed,0,num) + values[p].getString(syntax);
                        parsed = new char[toparse.length-i]; num = 0;
                    } else {
                        parsed[num++] = '?';
                        preparedParams.add(values[p]);
                    }
                    charParsed = params[p].length;
                    break;
                }
            }
            if(charParsed==0) {
                parsed[num++] = toparse[i];
                charParsed = 1;
            }
            i = i + charParsed;
        }
        parsedString = parsedString + new String(parsed,0,num);

        System.out.println(parsedString);

        PreparedStatement statement = connection.prepareStatement(parsedString);
        paramNum = 1;
        for(TypeObject param : preparedParams)
            param.writeParam(statement,paramNum++);

        return statement;
    }

    // вспомогательные методы
    public static String stringExpr(Map<String,String> keySelect,Map<String,String> propertySelect) {
        String expressionString = "";
        for(Map.Entry<String,String> Key : keySelect.entrySet())
            expressionString = (expressionString.length()==0?"":expressionString+",") + Key.getValue() + " AS " + Key.getKey();
        if(keySelect.size()==0)
            expressionString = "1 AS subkey";
        for(Map.Entry<String,String> Property : propertySelect.entrySet())
            expressionString = (expressionString.length()==0?"":expressionString+",") + Property.getValue() + " AS " + Property.getKey();
        return expressionString;
    }

    public static <T> LinkedHashMap<String,String> mapNames(Map<T,String> exprs,Map<T,String> names, List<T> order) {
        LinkedHashMap<String,String> result = new LinkedHashMap<String, String>();
        for(Map.Entry<T,String> name : names.entrySet()) {
            result.put(name.getValue(),exprs.get(name.getKey()));
            order.add(name.getKey());
        }
        return result;
    }
}
