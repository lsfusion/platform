package platform.server.data.expr.formula;

import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.formula.conversion.CompatibleTypeConversion;
import platform.server.data.expr.formula.conversion.TypeConversion;
import platform.server.data.type.Type;

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
            if (!(expr instanceof KeyExpr)) {
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
