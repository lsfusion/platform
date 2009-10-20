package platform.server.logics.properties;

import platform.interop.Compare;
import platform.server.data.classes.LogicalClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.where.WhereBuilder;

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

    public SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return new ValueExpr(true, LogicalClass.instance).and(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare));
    }
}
