package platform.server.logics.property.derived;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.DeconcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.FormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertyChanges;

public class DeconcatenateProperty extends FormulaProperty<DeconcatenateProperty.Interface> {
    
    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    final int part;
    final BaseClass baseClass;

    public DeconcatenateProperty(String sID, int part, BaseClass baseClass) {
        super(sID,"Concatenate "+part, SetFact.singletonOrder(new Interface(0)));
        
        this.part = part;
        this.baseClass = baseClass;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return DeconcatenateExpr.create(joinImplement.singleValue(),part,baseClass);
    }

    private DeconcatenateProperty.Interface getInterface() {
        return interfaces.single();
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        // так как не знаем соседних типов не можем построить valueclass
        return super.getInterfaceCommonClasses(commonValue);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
