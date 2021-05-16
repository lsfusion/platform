package lsfusion.server.data.sql.syntax;

import lsfusion.base.BaseUtils;

import java.sql.SQLException;
import java.sql.Types;

public class OracleSQLSyntax extends DefaultSQLSyntax {

    public final static OracleSQLSyntax instance = new OracleSQLSyntax();

    private OracleSQLSyntax() {
    }

    public boolean allowViews() {
        return true;
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE GLOBAL TEMPORARY TABLE "+ tableName +" ("+ declareString + ") ON COMMIT PRESERVE ROWS";
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        throw new RuntimeException("wrong update model");
    }

    public String getClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    public String isNULL(String exprs, boolean notSafe) {
        return "NVL("+ exprs +")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top, boolean distinct) {
        if(top.length()!=0)
            where = (where.length()==0?"": where +" AND ") + "rownum<=" + top;
        return "SELECT " + (distinct ? "DISTINCT " : "") + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + BaseUtils.clause("ORDER BY", orderBy);
        return "SELECT * FROM (" + union + ") WHERE rownum<=" + top + BaseUtils.clause("ORDER BY", orderBy);
    }

    public String getCommandEnd() {
        return "";
    }

    public String getClustered() {
        return "";
    }

    @Override
    public String getIntegerType() {
        return "NUMBER(5)";
    }
    @Override
    public int getIntegerSQL() {
        return Types.NUMERIC;
    }

    public int updateModel() {
        return 2;
    }

    public String getByteArrayType() {
        return "long raw";
    }

    public String getTextType() {
        return "clob";
    }

    public String getMetaName(String name) {
        return name.toUpperCase();
    }

    @Override
    public boolean isDeadLock(SQLException e) {
        return false;
    }

    @Override
    public boolean isUpdateConflict(SQLException e) {
        return false;
    }

    @Override
    public boolean isTimeout(SQLException e) {
        return false;
    }

    @Override
    public boolean isTransactionCanceled(SQLException e) {
        return false;
    }

    @Override
    public boolean isConnectionClosed(SQLException e) {
        return false;
    }

    @Override
    public boolean hasJDBCTimeoutMultiThreadProblem() {
        return true;
    }

    @Override
    public String getFieldName(String name) {
        return BaseUtils.packWords(super.getFieldName(name), 30);
    }

    @Override
    public String getTableName(String tableName) {
        return BaseUtils.packWords(super.getTableName(tableName), 30);
    }

    @Override
    public String getConstraintName(String name) {
        return BaseUtils.packWords(super.getConstraintName(name), 30);
    }

    @Override
    public String getIndexName(String name) {
        return BaseUtils.packWords(super.getIndexName(name), 30);
    }
}
