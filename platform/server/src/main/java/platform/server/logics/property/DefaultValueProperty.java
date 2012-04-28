package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ObjectValue;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Map;

public class DefaultValueProperty extends FormulaProperty<PropertyInterface>{

    private ObjectValue defaultValue;

    public DefaultValueProperty(String sID, ConcreteValueClass valueClass) {
        super(sID, "Значение по умолчанию для " + valueClass.toString(), new ArrayList<PropertyInterface>());

        defaultValue = ObjectValue.getValue(valueClass.getDefaultValue(), valueClass);

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return defaultValue.getExpr();
    }
}
