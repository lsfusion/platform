package platform.server.data.expr.query;

import platform.server.Settings;
import platform.server.classes.StringClass;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;

import java.util.Map;

public enum GroupType {
    SUM, MAX, ANY;

    public String getString(String expr, Type type, SQLSyntax syntax) {
        String func = null;
        switch (this) {
            case MAX:
                func = "MAX";
                break;
            case ANY:
//                return "MAX";
                func = "ANYVALUE";
                if(type instanceof StringClass)
                    expr = expr + "::" + type.getDB(syntax);
                break;
            case SUM:
                func = "SUM";
                break;
        }
        return func + "(" + expr + ")";
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
        switch (this) {
            case MAX:
                return new MaxGroupExpr(group, expr);
            case SUM:
                return new SumGroupExpr(group, expr);
            case ANY:
                return new AnyGroupExpr(group, expr);
        }
        throw new RuntimeException("can not be");
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
}
