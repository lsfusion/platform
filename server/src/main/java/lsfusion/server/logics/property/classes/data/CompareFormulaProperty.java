package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Iterator;
import java.util.function.IntFunction;

public class CompareFormulaProperty extends ValueFormulaProperty<CompareFormulaProperty.Interface> {

    public final Compare compare;
    public final Interface operator1;
    public final Interface operator2;

    public CompareFormulaProperty(Compare compare) {
        super(LocalizedString.create(compare.toString(), false), getInterfaces(2), LogicalClass.instance);

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
        return SetFact.toOrderExclSet(paramCount, Interface::new);
    }

    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ValueExpr.get(joinImplement.get(operator1).compare(joinImplement.get(operator2), compare));
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        if(inferSameClassCompare())
            return inferJoinInterfaceClasses(operator1, operator2, inferType);

        return new Inferred<>(interfaces.mapValues((IntFunction<ExClassSet>) index -> null));
    }

    public <T extends PropertyInterface> Inferred<T> inferJoinInterfaceClasses(PropertyInterfaceImplement<T> operator1, PropertyInterfaceImplement<T> operator2, InferType inferType) {
        assert inferSameClassCompare();

        Compared<T> compared;
        if(this.compare == Compare.EQUALS || this.compare == Compare.NOT_EQUALS)
            compared = new Equals<>(operator1, operator2);
        else
            compared = new Relationed<>(operator1, operator2);
        return Inferred.create(compared, inferType, this.compare == Compare.NOT_EQUALS);
    }

    public boolean inferSameClassCompare() {
        // should match getClassWhere (see for example CompareWhere.getMeanClassWhere / calculateClassWhere)
        switch(compare) {
            case CONTAINS:
            case MATCH:
            case INARRAY:
                return false;
        }
        return true;
    }
}
