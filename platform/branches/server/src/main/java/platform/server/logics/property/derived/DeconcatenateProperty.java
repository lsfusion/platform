package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.data.expr.DeconcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.FormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collections;
import java.util.Map;

public class DeconcatenateProperty extends FormulaProperty<DeconcatenateProperty.Interface> {
    
    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    int part;
    BaseClass baseClass;

    protected DeconcatenateProperty(int part, BaseClass baseClass) {
        super("DECONCATENATE_"+part,"Concatenate "+part, Collections.singletonList(new Interface(0)));
        
        this.part = part;
        this.baseClass = baseClass;
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return DeconcatenateExpr.create(BaseUtils.singleValue(joinImplement),part,baseClass);
    }
}
