package platform.server.data.expr.query;

import platform.server.Settings;
import platform.server.classes.StringClass;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;

import java.util.Map;

public enum GroupType implements AggrType {
    SUM, MAX, ANY;

    public String getString() {
        switch (this) {
            case MAX:
                return "MAX";
            case ANY:
                return "ANYVALUE";
            case SUM:
                return "SUM";
        }
        throw new RuntimeException("can not be");
    }

    public Expr add(Expr op1, Expr op2) {
        switch (this) {
            case MAX:
                return op1.max(op2);
            case SUM:
                return op1.sum(op2);
            case ANY: // для этого ANY и делается
                return op1.nvl(op2);
        }
        throw new RuntimeException("can not be");
    }

    public GroupExpr createExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        return new GroupExpr(group, expr, this);
    }

    public boolean splitExprCases() {
        return (this==MAX || this==ANY) && Settings.instance.isSplitGroupMaxExprcases();
    }

    public boolean splitInnerJoins() {
        return (this==MAX || this==ANY) && Settings.instance.isSplitMaxGroupInnerJoins();
    }

    public boolean noExclusive() {
        return (this==MAX || this==ANY);
    }

    public boolean isSelect() {
        return this==MAX || this==ANY;
    }

    public boolean canBeNull() {
        return false;
    }
}
