/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

interface SQLSyntax {

    boolean allowViews();

    String getUpdate(String TableString,String SetString,String FromString,String WhereString);

    void startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    String startTransaction();
    String commitTransaction();
    String rollbackTransaction();

    String isNULL(String Expr1, String Expr2, boolean NotSafe);

    String getClustered();
    String getCommandEnd();

    String getTop(int Top,String SelectString);

    String getNullValue(String DBType);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
}

abstract class DataAdapter implements SQLSyntax {

    static DataAdapter getDefault() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        return new PostgreDataAdapter();
    }

    public String getNullValue(String DBType) {
        return "NULL";
    }

    Connection Connection;

    public String getTop(int Top,String SelectString) {
        return (Top==0?"":"TOP "+Top+" ") + SelectString;
    }

    DataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        
        startConnection();
    }
    
    void CreateTable(Table Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.Keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare();
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare();
        CreateString = CreateString + ",CONSTRAINT PK_" + Table.Name + " PRIMARY KEY " + getClustered() + " (" + KeyString + ")";

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        Execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");
    }

    void Execute(String ExecuteString) throws SQLException {
        Statement Statement = Connection.createStatement();
//        System.out.println(ExecuteString+getCommandEnd());
        Statement.execute(ExecuteString+getCommandEnd());
    }
    
    void InsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {
        
        String InsertString = "";
        String ValueString = "";
        
        // пробежим по KeyFields'ам
        for(KeyField Key : Table.Keys) {
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }
        
        // пробежим по Fields'ам
        for(Field Prop : PropFields.keySet()) {
            Object Value = PropFields.get(Prop);
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+(Value==null?"NULL":(Value instanceof String?"'"+(String)Value+"'":Value.toString()));
        }

        Execute("INSERT INTO "+Table.Name+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> IsRecQuery = new JoinQuery<Object,String>();

        Join<KeyField,PropertyField> TableJoin = new Join<KeyField,PropertyField>(Table);
        // сначала закинем KeyField'ы и прогоним Select
        for(KeyField Key : Table.Keys)
            TableJoin.Joins.put(Key,new ValueSourceExpr(KeyFields.get(Key)));

        IsRecQuery.Wheres.add(new JoinWhere(TableJoin));

        if(IsRecQuery.executeSelect(this).size()>0) {
            // есть запись нужно Update лупить
            UpdateRecords(new ModifyQuery(Table,new DumbSource<KeyField,PropertyField>(KeyFields,PropFields)));
        } else
            // делаем Insert
            InsertRecord(Table,KeyFields,PropFields);
    }

    void deleteKeyRecords(Table Table,Map<KeyField,Integer> Keys) throws SQLException {
 //       Execute(Table.GetDelete());
        String DeleteWhere = "";
        for(Map.Entry<KeyField,Integer> DeleteKey : Keys.entrySet())
            DeleteWhere = (DeleteWhere.length()==0?"":DeleteWhere+" AND ") + DeleteKey.getKey().Name + "=" + DeleteKey.getValue();

        Execute("DELETE FROM "+Table.Name+(DeleteWhere.length()==0?"":" WHERE "+DeleteWhere));
    }

    void UpdateRecords(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getUpdate(this));
    }

    void InsertSelect(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getInsertSelect(this));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    void ModifyRecords(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getInsertLeftKeys(this));
        Execute(Modify.getUpdate(this));
    }

    void Disconnect() throws SQLException {
        Connection.close();
    }

    // по умолчанию
    public String getClustered() {
        return "CLUSTERED ";
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return true;
    }

    public String getCommandEnd() {
        return "";
    }

}

class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter()  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
    }

    public boolean allowViews() {
        return false;
    }

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        return TableString + "," + FromString + SetString + WhereString;
    }

    public void startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        java.lang.Class.forName("com.mysql.jdbc.Driver");
        Connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TestPlat");

        try {
            Execute("DROP DATABASE testplat");
        } catch(Exception e) {
        }

        Execute("CREATE DATABASE testplat");
        Execute("USE testplat");
    }

    public String startTransaction() {
        return "START TRANSACTION";
    }

    public String commitTransaction() {
        return "COMMIT";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
        return "IFNULL(" + Expr1 + "," + "Expr2" + ")";
    }
}

class MSSQLDataAdapter extends DataAdapter {

    MSSQLDataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        return TableString + SetString + " FROM " + FromString + WhereString;
    }

    public void startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver");
        Connection = DriverManager.getConnection("jdbc:jtds:sqlserver://server:1433;namedPipe=true;User=sa;Password=");

        try {
            Execute("DROP DATABASE testplat");
        } catch(Exception e) {
        }

        Execute("CREATE DATABASE testplat");
        Execute("USE TestPlat");
    }

    public String startTransaction() {
        return "BEGIN TRANSACTION";
    }

    public String commitTransaction() {
        return "COMMIT TRANSACTION";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
        if(NotSafe)
            return "CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END";
        else
            return "ISNULL("+Expr1+","+Expr2+")";
    }

    public boolean isNullSafe() {
        return false;
    }
}

class PostgreDataAdapter extends DataAdapter {

    PostgreDataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        return TableString + SetString + " FROM " + FromString + WhereString;
    }

    public void startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        java.lang.Class.forName("org.postgresql.Driver");
        Connection = DriverManager.getConnection("jdbc:postgresql://server/postgres?user=postgres&password=11111");

        try {
            Execute("DROP DATABASE testplat");
        } catch(Exception e) {
        }

        Execute("CREATE DATABASE testplat");

        Connection.close();

        Connection = DriverManager.getConnection("jdbc:postgresql://server/testplat?user=postgres&password=11111");
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
    }

    public String getNullValue(String DBType) {
        String EmptyValue = (DBType.equals("integer")?"0":"''");
        return "NULLIF(" + EmptyValue + "," + EmptyValue + ")";
    }

    public String startTransaction() {
        return "BEGIN TRANSACTION";
    }

    public String commitTransaction() {
        return "COMMIT TRANSACTION";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    public String getTop(int Top,String SelectString) {
        return SelectString + (Top==0?"":" LIMIT "+Top);
    }

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE("+Expr1+","+Expr2+")";
    }
}
