package lsfusion.server.data.sql.syntax;

import lsfusion.base.BaseUtils;

public class MySQLSQLSyntax extends DefaultSQLSyntax {

    public final static MySQLSQLSyntax instance = new MySQLSQLSyntax();

    private MySQLSQLSyntax() {
    }

    public boolean allowViews() {
        return false;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + "," + fromString + setString + whereString;
    }

    public String getClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    public String isNULL(String exprs, boolean notSafe) {
        return "IFNULL(" + exprs + ")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top, String offset, boolean distinct) {
        return "SELECT " + (distinct ? "DISTINCT " : "") + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }

    public String getUnionOrder(String union, String orderBy, String top, String offset) {
        return union + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }

    @Override
    public String getAnalyze(String table) {
        return "ANALYZE TABLE " + table;
    }

    @Override
    public String getVarStringType(int length) {
        return "char(" + length + ")";
    }

    @Override
    public String getBPTextType() {
        return "char";
    }
}
