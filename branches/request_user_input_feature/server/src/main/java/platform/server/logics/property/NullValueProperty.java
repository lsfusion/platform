package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NullValueProperty extends FormulaProperty<PropertyInterface>{

    public NullValueProperty() {
        super("nullValue", "Значение NULL", new ArrayList<PropertyInterface>());

        finalizeInit();
    }

    @Override
    public Map<PropertyInterface, ValueClass> getMapClasses() { // временно так (пока для определния сигнатур action'ов)
        return new HashMap<PropertyInterface, ValueClass>();
    }

    @Override
    public ValueClass getValueClass() { // временно так (пока для определния сигнатур action'ов)
        return null;
    }

    @Override
    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }
}
