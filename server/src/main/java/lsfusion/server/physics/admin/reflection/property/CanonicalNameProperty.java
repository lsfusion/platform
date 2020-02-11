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
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CanonicalNameProperty extends FormulaProperty<CanonicalNameProperty.Interface> {
    private final LAP property;
    private StringClass valueClass = StringClass.get(false, 200);

    public CanonicalNameProperty(LAP property) {
        super(LocalizedString.create("canonical name", false), SetFact.EMPTYORDER());
        this.property = property;
    }

    @Override
    protected Expr calculateExpr(ImMap joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return property == null ? null : valueClass.getStaticExpr(LocalizedString.create(property.getActionOrProperty().getCanonicalName(), false));
    }

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toExValue(valueClass);
    }
}
