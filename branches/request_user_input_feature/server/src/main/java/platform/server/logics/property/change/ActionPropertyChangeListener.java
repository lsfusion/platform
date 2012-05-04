package platform.server.logics.property.change;

import platform.server.classes.ActionClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.StaticValueExpr;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;

public class ActionPropertyChangeListener<P extends PropertyInterface> extends PropertyChangeListener<P> {
    public final static StaticValueExpr ACTION_TRUE_EXPR = new StaticValueExpr(true, ActionClass.instance);

    public ActionPropertyChangeListener(Property<P> property, PropertyImplement<ClassPropertyInterface, P> actionImplement) {
        super(property, actionImplement);
    }

    public ActionPropertyChangeListener(Property<P> property, PropertyInterface valueInterface, PropertyImplement<ClassPropertyInterface, PropertyInterface> actionListener) {
        super(property, valueInterface, actionListener);
    }

    @Override
    protected Expr getValueExpr() {
        return ACTION_TRUE_EXPR;
    }
}
