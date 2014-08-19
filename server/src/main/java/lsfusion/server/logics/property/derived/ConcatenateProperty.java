package lsfusion.server.logics.property.derived;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.OrConcatenateClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.classes.sets.ResolveConcatenateClassSet;
import lsfusion.server.data.expr.ConcatenateExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.FormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.infer.*;
import lsfusion.server.logics.property.infer.ExClassSet;
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

    public ConcatenateProperty(int intNum) {
        super("Concatenate " + intNum, getInterfaces(intNum));

        finalizeInit();
    }

    public Interface getInterface(int i) {
        Iterator<Interface> it = interfaces.iterator();
        for(int j=0;j<i;j++)
            it.next();
        return it.next();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        ImList<Expr> exprs = getOrderInterfaces().mapListValues(new GetValue<Expr, Interface>() {
            public Expr getMapValue(Interface value) {
                return joinImplement.get(value);
            }});
        return ConcatenateExpr.create(exprs);
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        if(commonValue!=null) {
            return new Inferred<Interface>(getOrderInterfaces().mapOrderValues(new GetIndex<ExClassSet>() {
                public ExClassSet getMapValue(int i) {
                    return getPart(i, commonValue);
                }}));
        }
        return super.calcInferInterfaceClasses(commonValue, inferType);
    }

    public static ExClassSet getPart(int i, ExClassSet commonValue) {
        return new ExClassSet(((ResolveConcatenateClassSet)ExClassSet.fromEx(commonValue)).get(i), commonValue.orAny);
    }

    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        if(inferred.size() == interfaces.size() && !inferred.containsNull() && inferred.filterFnValues(new SFunctionSet<ExClassSet>() { // жесть конечно, но потом будем уточнять этот метод
            public boolean contains(ExClassSet element) {
                return element.orAny;
            }}).isEmpty()) {
            ImList<ResolveClassSet> andClassSets = getOrderInterfaces().mapList(ExClassSet.fromExAnd(inferred));
            return new ExClassSet(new ResolveConcatenateClassSet(andClassSets.toArray(new ResolveClassSet[andClassSets.size()])), false);
        }
        return super.calcInferValueClass(inferred, inferType);
    }
}
