package lsfusion.server.data.sql.syntax;

public class UnknownSQLSyntax extends DefaultSQLSyntax {

    public final static UnknownSQLSyntax instance = new UnknownSQLSyntax();

    private UnknownSQLSyntax() {
    }

    @Override
    public boolean allowViews() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClassName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String isNULL(String exprs, boolean notSafe) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top, boolean distinct) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUnionOrder(String union, String orderBy, String top) {
        throw new UnsupportedOperationException();
    }
}
