package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.interop.Compare;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Iterator;

public class CompareFormulaProperty extends ValueFormulaProperty<CompareFormulaProperty.Interface> {

    final Compare compare;
    public final Interface operator1;
    public final Interface operator2;

    public CompareFormulaProperty(String sID, Compare compare) {
        super(sID, compare.toString(), getInterfaces(2), LogicalClass.instance);

        this.compare = compare;
        Iterator<Interface> i = interfaces.iterator();
        operator1 = i.next();
        operator2 = i.next();

        finalizeInit();
    }

    public static class Interface extends PropertyInterface {
        
        Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int paramCount) {
        return SetFact.toOrderExclSet(paramCount, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }});
    }

    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare));
    }
}
