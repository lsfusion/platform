package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.Compare;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.infer.*;
import lsfusion.server.session.PropertyChanges;

import java.util.Iterator;

public class CompareFormulaProperty extends ValueFormulaProperty<CompareFormulaProperty.Interface> {

    public final Compare compare;
    public final Interface operator1;
    public final Interface operator2;

    public CompareFormulaProperty(Compare compare) {
        super(LocalizedString.createFromSimpleString(compare.toString()), getInterfaces(2), LogicalClass.instance);

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

    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare));
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return inferJoinInterfaceClasses(operator1, operator2, inferType);
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.logical;
    }
    
    public <T extends PropertyInterface> Inferred<T> inferJoinInterfaceClasses(CalcPropertyInterfaceImplement<T> operator1, CalcPropertyInterfaceImplement<T> operator2, InferType inferType) {
        Compared<T> compared;
        if(this.compare == Compare.EQUALS || this.compare == Compare.NOT_EQUALS)
            compared = new Equals<>(operator1, operator2);
        else
            compared = new Relationed<>(operator1, operator2);
        return Inferred.create(compared, inferType, this.compare == Compare.NOT_EQUALS);
    }
}
