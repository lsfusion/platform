package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.ConcatenateExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveConcatenateClassSet;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Iterator;
import java.util.function.IntFunction;

public class ConcatenateProperty extends FormulaProperty<ConcatenateProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, Interface::new);
    }

    public ConcatenateProperty(int intNum) {
        super(LocalizedString.create("Concatenate " + intNum), getInterfaces(intNum));

        finalizeInit();
    }

    public Interface getInterface(int i) {
        Iterator<Interface> it = interfaces.iterator();
        for(int j=0;j<i;j++)
            it.next();
        return it.next();
    }

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        ImList<Expr> exprs = getOrderInterfaces().mapListValues(joinImplement::get);
        return ConcatenateExpr.create(exprs);
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        if(commonValue!=null) {
            return new Inferred<>(getOrderInterfaces().mapOrderValues(new IntFunction<ExClassSet>() {
                public ExClassSet apply(int i) {
                    return getPart(i, commonValue);
                }
            }));
        }
        return super.calcInferInterfaceClasses(commonValue, inferType);
    }

    public static ExClassSet getPart(int i, ExClassSet commonValue) {
        return new ExClassSet(((ResolveConcatenateClassSet)ExClassSet.fromEx(commonValue)).get(i), commonValue.orAny);
    }

    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        // жесть конечно, но потом будем уточнять этот метод
        if(inferred.size() == interfaces.size() && !inferred.containsNull() && inferred.filterFnValues(element -> element.orAny).isEmpty()) {
            ImList<ResolveClassSet> andClassSets = getOrderInterfaces().mapList(ExClassSet.fromExAnd(inferred));
            return new ExClassSet(new ResolveConcatenateClassSet(andClassSets.toArray(new ResolveClassSet[andClassSets.size()])), false);
        }
        return ExClassSet.FALSE;
    }
}
