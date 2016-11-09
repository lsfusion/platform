package lsfusion.server.logics.property.derived;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.expr.DeconcatenateExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.FormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.session.PropertyChanges;

public class DeconcatenateProperty extends FormulaProperty<DeconcatenateProperty.Interface> {
    
    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    final int part;
    final BaseClass baseClass;

    public DeconcatenateProperty(int part, BaseClass baseClass) {
        super("Concatenate "+part, SetFact.singletonOrder(new Interface(0)));
        
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
        // так как не знаем соседних типов не можем построить valueclass
        return super.calcInferInterfaceClasses(commonValue, inferType);
    }
    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        ExClassSet exClassSet = inferred.get(getInterface());
        if(exClassSet != null)
            return ConcatenateProperty.getPart(part, exClassSet);            
        return super.calcInferValueClass(inferred, inferType);
    }
}
