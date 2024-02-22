package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.DeconcatenateExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.IntFunction;

public class DeconcatenateProperty extends FormulaProperty<DeconcatenateProperty.Interface> {
    
    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    final int part;
    final BaseClass baseClass;

    public DeconcatenateProperty(int part, BaseClass baseClass) {
        super(LocalizedString.create("Concatenate " + part), SetFact.singletonOrder(new Interface(0)));
        
        this.part = part;
        this.baseClass = baseClass;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return DeconcatenateExpr.create(joinImplement.singleValue(),part,baseClass);
    }

    private DeconcatenateProperty.Interface getInterface() {
        return interfaces.single();
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(interfaces.mapValues((IntFunction<ExClassSet>) index -> null));
    }
    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        ExClassSet exClassSet = inferred.get(getInterface());
        if(exClassSet != null)
            return ConcatenateProperty.getPart(part, exClassSet);            
        return ExClassSet.FALSE;
    }
}
