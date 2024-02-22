package lsfusion.server.physics.admin.reflection.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.StaticValueProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// it seems, that can be refactored to ValueProperty with "delayed" calculation
public class CanonicalNameProperty extends StaticValueProperty {
    private final LAP property;
    private StringClass valueClass = StringClass.get(false, 200);

    public CanonicalNameProperty(LAP property) {
        super(LocalizedString.create("canonical name", false));
        this.property = property;
    }

    @Override
    protected Expr calculateExpr(ImMap joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return valueClass.getStaticExpr(getStaticValue());
    }

    @Override
    protected Inferred<PropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return StaticValueProperty.inferInterfaceClasses(inferType, commonValue);
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    public Object getStaticValue() {
        return LocalizedString.create(property.getActionOrProperty().getCanonicalName(), false);
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<PropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toExValue(valueClass);
    }
}
