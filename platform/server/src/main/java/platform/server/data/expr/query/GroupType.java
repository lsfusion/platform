package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.Settings;
import platform.server.classes.StringClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.logics.property.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum GroupType implements AggrType {
    SUM, MAX, MIN, ANY, STRING_AGG, AGGAR_SETADD;

    public String getString() {
        switch (this) {
            case MAX:
                return "MAX";
            case MIN:
                return "MIN";
            case ANY:
                return "ANYVALUE";
            case SUM:
                return "SUM";
            case STRING_AGG:
                return "STRING_AGG";
            case AGGAR_SETADD:
                return "AGGAR_SETADD";
        }
        throw new RuntimeException("can not be");
    }

    public <T extends PropertyInterface> GroupProperty<T> createProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        switch (this) {
            case MAX:
                return new MaxGroupProperty<T>(sID, caption, interfaces, property, false);
            case MIN:
                return new MaxGroupProperty<T>(sID, caption, interfaces, property, true);
            case SUM:
                return new SumGroupProperty<T>(sID, caption, interfaces, property);
        }
        throw new RuntimeException("not supported");
    }

    public Expr add(Expr op1, Expr op2) {
        switch (this) {
            case MAX:
                return op1.max(op2);
            case MIN:
                return op1.min(op2);
            case SUM:
                return op1.sum(op2);
            case ANY: // для этого ANY и делается
                return op1.nvl(op2);
        }
        throw new RuntimeException("can not be");
    }

    public boolean isSelect() {
        return this==MAX || this==MIN || this==ANY;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean hasAdd() {
        return this!=STRING_AGG && this!=AGGAR_SETADD;
    }

    public boolean splitExprCases() {
        assert hasAdd();
        return isSelect() && Settings.instance.isSplitGroupSelectExprcases();
    }

    public boolean splitInnerJoins() {
        assert hasAdd();
        return isSelect() && Settings.instance.isSplitSelectGroupInnerJoins();
    }

    public boolean splitInnerCases() {
        assert hasAdd();
        return false;
    }

    public boolean exclusive() {
        assert hasAdd();
        return !isSelect();
    }

    public String getSource(List<String> exprs, OrderedMap<String, Boolean> orders, Set<String> ordersNotNull, Type type, SQLSyntax syntax) {
        String result = getString() + "(" + BaseUtils.toString(exprs, ",") + BaseUtils.clause("ORDER BY", Query.stringOrder(orders, ordersNotNull, syntax)) + ")";
        if(this==SUM)
            result = "notZero(" + result + ")";
        return result;
    }

    public Expr getSingleExpr(List<Expr> exprs, OrderedMap<Expr, Boolean> orders) {
        return exprs.iterator().next();
    }

    public int numExprs() {
        if(this==STRING_AGG)
            return 2;
        else
            return 1;
    }

    public Type getType(Type exprType) {
        if(this==STRING_AGG)
            return ((StringClass)exprType).extend(10);
        else
            return exprType;
    }
}
