package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.conversion.CompatibleTypeConversion;
import lsfusion.server.data.expr.formula.conversion.TypeConversion;
import lsfusion.server.data.type.Type;

public abstract class AbstractFormulaImpl implements FormulaImpl {
    protected final TypeConversion conversion;

    public AbstractFormulaImpl() {
        this(CompatibleTypeConversion.instance);
    }

    public AbstractFormulaImpl(TypeConversion conversion) {
        this.conversion = conversion;
    }

    public static Type getCompatibleType(ExprType source, TypeConversion conversion) {
        Type type = null;
        for (int i = 0; i < source.getExprCount(); ++i) {
            if (!source.isParam(i)) {
                Type exprType = source.getType(i);
                if (type == null) {
                    type = exprType;
                } else {
                    Type conversionType = conversion.getType(type, exprType);
                    if (conversionType != null) {
                        type = conversionType;
                    }
                }
            }
        }
        return type;
    }

    @Override
    public Type getType(ExprType source) {
        return getCompatibleType(source, conversion);
    }
}
