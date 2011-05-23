package platform.server.data.expr.query;

import platform.server.data.sql.SQLSyntax;

public enum OrderType {
    SUM, PREVIOUS;

    public String getSource(SQLSyntax syntax) {
        switch(this) {
            case SUM:
                return "SUM";
            case PREVIOUS:
                return "lag";
        }
        throw new RuntimeException();
    }
}
