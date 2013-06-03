package lsfusion.server.logics.property.derived;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ConcatenateValueClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.ConcatenateExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.FormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.PropertyChanges;

import java.util.Iterator;

public class ConcatenateProperty extends FormulaProperty<ConcatenateProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }});
    }

    public ConcatenateProperty(String sID, int intNum) {
        super(sID, "Concatenate " + intNum, getInterfaces(intNum));

        finalizeInit();
    }

    public Interface getInterface(int i) {
        Iterator<Interface> it = interfaces.iterator();
        for(int j=0;j<i;j++)
            it.next();
        return it.next();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        ImList<Expr> exprs = getOrderInterfaces().mapListValues(new GetValue<Expr, Interface>() {
            public Expr getMapValue(Interface value) {
                return joinImplement.get(value);
            }});
        return ConcatenateExpr.create(exprs);
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(final ValueClass commonValue) {
        if(commonValue!=null) {
            return getOrderInterfaces().mapOrderValues(new GetIndex<ValueClass>() {
                public ValueClass getMapValue(int i) {
                    return ((ConcatenateValueClass)commonValue).get(i);
                }});
        }
        return super.getInterfaceCommonClasses(commonValue);
    }
}
