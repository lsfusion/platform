package lsfusion.server.logics.property.classes.data;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.IncrementUnionProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class IncrementFormulaUnionProperty extends IncrementUnionProperty {

    public IncrementFormulaUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces) {
        super(caption, interfaces);
    }

    @Override
    protected ExClassSet calcInferOperandClass(ExClassSet commonValue, int index) {
        return FormulaJoinProperty.inferInterfaceClass(commonValue, null, index); // formula should be implemented, but since it is not used inside - we won't do that for now
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        // alson inferValueClass maybe should be used, but we'll need formulas for that so we'll leave it that way
        return ExClassSet.removeValues(super.calcInferValueClass(inferred, inferType));
    }
}
