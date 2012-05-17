package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcatenateValueClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.DeconcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.FormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertyChanges;

import java.util.Collections;
import java.util.Map;

public class DeconcatenateProperty extends FormulaProperty<DeconcatenateProperty.Interface> {
    
    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    final int part;
    final BaseClass baseClass;

    public DeconcatenateProperty(String sID, int part, BaseClass baseClass) {
        super(sID,"Concatenate "+part, Collections.singletonList(new Interface(0)));
        
        this.part = part;
        this.baseClass = baseClass;

        finalizeInit();
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return DeconcatenateExpr.create(BaseUtils.singleValue(joinImplement),part,baseClass);
    }

    private DeconcatenateProperty.Interface getInterface() {
        return BaseUtils.single(interfaces);
    }

    @Override
    public Map<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        // так как не знаем соседних типов не можем построить valueclass
        return super.getInterfaceCommonClasses(commonValue);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
