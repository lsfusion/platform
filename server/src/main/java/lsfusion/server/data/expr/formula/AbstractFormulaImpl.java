package lsfusion.server.data.expr.formula;

import lsfusion.server.caches.ParamExpr;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
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

    public static Type getCompatibleType(ExprSource source, TypeConversion conversion) {
        Type type = null;
        for (int i = 0; i < source.getExprCount(); ++i) {
            Expr expr = source.getExpr(i);
            if (!(expr instanceof ParamExpr)) {
                Type exprType = expr.getSelfType();
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
    public ConcreteClass getStaticClass(ExprSource source) {
        return (ConcreteClass) getCompatibleType(source, conversion);
    }

    @Override
    public Type getType(ExprSource source, KeyType keyType) {
        return getCompatibleType(source, conversion);
    }
}
