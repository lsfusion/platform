package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.ConcatenateUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ConcatenateUnionProperty extends UnionProperty {

    private final String separator;
    private final ImList<PropertyInterfaceImplement<Interface>> operands;

    public ConcatenateUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImList<PropertyInterfaceImplement<Interface>> operands, String separator) {
        super(caption, interfaces);
        this.separator = separator;
        this.operands = operands;

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImCol<Expr> exprs = getOperands().mapColValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));
        return FormulaUnionExpr.create(getFormula(), exprs.toList());
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return FormulaImplProperty.inferValueClass(getOrderInterfaces(), getFormula(), inferred);
    }

    @IdentityLazy
    protected FormulaUnionImpl getFormula() {
        if(separator != null)
            return new StringConcatenateFormulaImpl(separator);
        else
            return JSONMergeFormulaImpl.instance;
    }

    @Override
    public ImCol<PropertyInterfaceImplement<Interface>> getOperands() {
        return operands.getCol();
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new ConcatenateUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.concat.union}"), this, LM
        );
    }
}