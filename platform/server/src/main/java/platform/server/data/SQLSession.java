package platform.server.data;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.query.Query;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.TypeObject;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.*;
import java.util.*;

public class SQLSession {

    public SQLSyntax syntax;
    
    private Connection connection;

    public SQLSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connection = adapter.startConnection();
        connection.setAutoCommit(true);
    }

    public void startTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }

    public void rollbackTransaction() throws SQLException {
        connection.rollback();

        connection.setAutoCommit(true);
    }

    public void commitTransaction() throws SQLException {
        connection.commit();

        connection.setAutoCommit(true);
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
        execute("ALTER TABLE " + table + " ADD " + field.getDeclare(syntax)); //COLUMN 
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
        System.out.println(executeString);

        Statement statement = connection.createStatement();
        try {
            statement.execute(executeString);
        } catch(SQLException e) {
            System.out.println(statement.toString());
            throw e;
        } finally {
            statement.close();
        }
    }

    private int executeDML(SQLExecute execute) throws SQLException {
        return executeDML(execute.command, execute.params);
    }

    private int executeDML(String command, Map<String, TypeObject> paramObjects) throws SQLException {
        PreparedStatement statement = getStatement(command,paramObjects);
        
//        System.out.println(statement);
        int result;
        try {
            result = statement.executeUpdate();
        } catch(SQLException e) {
            System.out.println(statement.toString());
            throw e;
        } finally {
            statement.close();
        }

        return result;
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

        executeDML("INSERT INTO "+table.getName(syntax)+" ("+insertString+") VALUES ("+valueString+")", params);
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
        Query<Object,String> isRecQuery = new Query<Object,String>(new HashMap<Object, KeyExpr>());

        // сначала закинем KeyField'ы и прогоним Select
        isRecQuery.and(table.joinAnd(DataObject.getMapValueExprs(keyFields)).getWhere());

        return isRecQuery.execute(this).size()>0;
    }

    public void ensureRecord(Table table,Map<KeyField,DataObject> keyFields,Map<PropertyField,ObjectValue> propFields) throws SQLException {
        if(!isRecord(table, keyFields))
            insertRecord(table,keyFields,propFields);
    }

    public void updateInsertRecord(Table table,Map<KeyField, DataObject> keyFields,Map<PropertyField,ObjectValue> propFields) throws SQLException {

        if(isRecord(table, keyFields)) {
            Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(table);
            updateQuery.putKeyWhere(keyFields);
            updateQuery.properties.putAll(ObjectValue.getMapExprs(propFields));

            // есть запись нужно Update лупить
            updateRecords(new ModifyQuery(table,updateQuery));
        } else
            // делаем Insert
            insertRecord(table,keyFields,propFields);
    }

    public Object readRecord(Table table,Map<KeyField,DataObject> keyFields,PropertyField field) throws SQLException {
        // по сути пустое кол-во ключей
        Query<Object,String> getQuery = new Query<Object,String>(new HashMap<Object,KeyExpr>());

        // сначала закинем KeyField'ы и прогоним Select
        Expr fieldExpr = table.joinAnd(DataObject.getMapValueExprs(keyFields)).getExpr(field);
        getQuery.properties.put("result",fieldExpr);
        getQuery.and(fieldExpr.getWhere());
        OrderedMap<Map<Object, Object>, Map<String, Object>> result = getQuery.execute(this);
        if(result.size()>0)
            return result.singleValue().get("result");
        else
            return null;
    }

    public void deleteKeyRecords(Table table,Map<KeyField,?> keys) throws SQLException {
        String deleteWhere = "";
        for(Map.Entry<KeyField,?> deleteKey : keys.entrySet())
            deleteWhere = (deleteWhere.length()==0?"":deleteWhere+" AND ") + deleteKey.getKey().name + "=" + deleteKey.getValue();

        execute("DELETE FROM "+ table.getName(syntax)+(deleteWhere.length()==0?"":" WHERE "+deleteWhere));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getUpdate(syntax));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getInsertSelect(syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public int modifyRecords(ModifyQuery modify) throws SQLException {
        executeDML(modify.getInsertLeftKeys(syntax));
        return updateRecords(modify);
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

    public static <T> OrderedMap<String,String> mapNames(Map<T,String> exprs,Map<T,String> names, List<T> order) {
        OrderedMap<String,String> result = new OrderedMap<String, String>();
        for(Map.Entry<T,String> name : names.entrySet()) {
            result.put(name.getValue(),exprs.get(name.getKey()));
            order.add(name.getKey());
        }
        return result;
    }
}
