package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.WhereBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class CompareFormulaProperty extends ValueFormulaProperty<FormulaPropertyInterface> {

    Compare compare;
    public FormulaPropertyInterface operator1;
    public FormulaPropertyInterface operator2;

    public CompareFormulaProperty(String sID, Compare compare) {
        super(sID, compare.toString(), getInterfaces(2), LogicalClass.instance);

        this.compare = compare;
        Iterator<FormulaPropertyInterface> i = interfaces.iterator();
        operator1 = i.next();
        operator2 = i.next();
    }

    static Collection<FormulaPropertyInterface> getInterfaces(int paramCount) {
        Collection<FormulaPropertyInterface> interfaces = new ArrayList<FormulaPropertyInterface>();
        for(int i=0;i<paramCount;i++)
            interfaces.add(new FormulaPropertyInterface(i));
        return interfaces;
    }

    public Expr calculateExpr(Map<FormulaPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare));
    }
}
