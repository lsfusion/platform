package lsfusion.server.data.sql.syntax;


import lsfusion.base.BaseUtils;

public class FirebirdSQLSyntax extends DefaultSQLSyntax {

    public final static FirebirdSQLSyntax instance = new FirebirdSQLSyntax();

    private FirebirdSQLSyntax() {
    }

    @Override
    public boolean allowViews() {
        return false;
    }

    @Override
    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClassName() {
        return "org.firebirdsql.jdbc.FBDriver";
    }

    @Override
    public String isNULL(String exprs, boolean notSafe) {
        return "COALESCE(" + exprs + ")";
    }

    @Override
    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top, String offset, boolean distinct) {
        return "SELECT " + (distinct ? "DISTINCT " : "") + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) +
                BaseUtils.clause("GROUP BY", groupBy) +
                BaseUtils.clause("HAVING", having) +
                BaseUtils.clause("ORDER BY", orderBy) +
                BaseUtils.clause("FIRST", top) +
                BaseUtils.clause("SKIP", offset);
    }

    @Override
    public String getUnionOrder(String union, String orderBy, String top, String offset) {
        return union + BaseUtils.clause("ORDER BY", orderBy) +
                BaseUtils.clause("FIRST", top) + BaseUtils.clause("SKIP", offset);
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE GLOBAL TEMPORARY TABLE "+ tableName +" ("+ declareString + ") ON COMMIT PRESERVE ROWS";
    }

    public String getFieldName(String fieldName) {
        // row is reserved, it must be escaped. used in SQLSession.uploadTableToConnection
        return fieldName.equals("row") ? escapeID(fieldName) : fieldName;
    }

    @Override
    public String getAnalyze(String table) {
        return "EXECUTE BLOCK AS " +
                "DECLARE variable_index_name VARCHAR(31); " +
                "BEGIN " +
                "  FOR SELECT RDB$INDEX_NAME " +
                "      FROM RDB$INDICES " +
                "      WHERE RDB$RELATION_NAME = '" + table + "'" +
                "      INTO :variable_index_name DO " +
                "  BEGIN " +
                "    EXECUTE STATEMENT 'SET STATISTICS INDEX ' || variable_index_name || ';'; " +
                "  END " +
                "END;";
    }

    public String getClustered() {
        return "";
    }
}

