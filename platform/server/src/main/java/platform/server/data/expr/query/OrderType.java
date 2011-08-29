package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.server.data.sql.SQLSyntax;

import java.util.List;

public enum OrderType implements AggrType {
    SUM, DISTR_CUM_PROPORTION, DISTR_RESTRICT, DISTR_RESTRICT_OVER, PREVIOUS;

    public String getFunc(SQLSyntax syntax) {
        switch(this) {
            case PREVIOUS:
                return "lag";
            default:
                return toString();
        }
    }

    public String getSource(SQLSyntax syntax, List<String> sources) {
        return getFunc(syntax) + "(" + BaseUtils.toString(sources, ",") + ")";
    }

    public boolean isSelect() {
        return this==PREVIOUS;
    }

    public boolean canBeNull() { // может возвращать null если само выражение не null
        return this==PREVIOUS;
    }
}
