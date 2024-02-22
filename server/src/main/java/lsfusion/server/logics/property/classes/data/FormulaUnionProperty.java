package lsfusion.server.logics.property.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.ConcatenateUnionDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class FormulaUnionProperty extends UnionProperty {

    public final FormulaUnionImpl formula;
    private final ImList<PropertyInterfaceImplement<Interface>> operands;

    // FORMULA
    // not pretty, but otherwise we need more complicated class structure (the same thing is in FormulaJoinProperty now)
    public FormulaUnionProperty(DataClass valueClass, CustomFormulaSyntax formula, int paramCount) {
        this(valueClass, formula, getInterfaces(paramCount));
    }
    private FormulaUnionProperty(DataClass valueClass, CustomFormulaSyntax formula, ImOrderSet<Interface> interfaces) {
        this(LocalizedString.create(formula.getDefaultSyntax()), interfaces, BaseUtils.immutableCast(interfaces), FormulaExpr.createUnionCustomFormulaImpl(formula, valueClass, interfaces.mapOrderSetValues(anInterface -> FormulaJoinProperty.getParamName(String.valueOf(anInterface.ID + 1)))));
    }

    // FORMULA, CONCAT, JSONBUILD
    public FormulaUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, ImList<PropertyInterfaceImplement<Interface>> operands, FormulaUnionImpl formula) {
        super(caption, interfaces);

        this.formula = formula;
        this.operands = operands;

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return FormulaUnionExpr.create(formula, operands.mapListValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere)));
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    @Override
    protected ExClassSet calcInferOperandClass(ExClassSet commonValue, int index) {
        return FormulaJoinProperty.inferInterfaceClass(commonValue, formula, index);
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return FormulaJoinProperty.inferValueClass(formula, operands.mapListValues(mapImpl -> mapImpl.mapInferValueClass(inferred, inferType)));
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
    public DrillDownFormEntity createDrillDownForm(BaseLogicsModule LM) {
        return new ConcatenateUnionDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.concat.union}"), this, LM
        );
    }
}