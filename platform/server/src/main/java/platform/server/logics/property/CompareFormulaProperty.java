package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.WhereBuilder;

import java.util.*;

public class CompareFormulaProperty extends ValueFormulaProperty<CompareFormulaProperty.Interface> {

    Compare compare;
    public Interface operator1;
    public Interface operator2;

    public CompareFormulaProperty(String sID, Compare compare) {
        super(sID, compare.toString(), getInterfaces(2), LogicalClass.instance);

        this.compare = compare;
        Iterator<Interface> i = interfaces.iterator();
        operator1 = i.next();
        operator2 = i.next();
    }

    public static class Interface extends PropertyInterface {
        
        Interface(int ID) {
            super(ID);
        }
    }

    static List<Interface> getInterfaces(int paramCount) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<paramCount;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    public Expr calculateExpr(Map<CompareFormulaProperty.Interface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare));
    }
}
