package platform.server.data.expr.formula;

import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.formula.conversion.CompatibleTypeConversion;
import platform.server.data.expr.formula.conversion.TypeConversion;
import platform.server.data.type.Type;

public abstract class AbstractFormulaImpl implements FormulaImpl {
    protected final TypeConversion conversions[];

    public AbstractFormulaImpl() {
        this(new TypeConversion[]{CompatibleTypeConversion.instance});
    }

    public AbstractFormulaImpl(TypeConversion[] conversions) {
        this.conversions = conversions;
    }

    public static Type getCompatibleType(ExprSource source, TypeConversion... conversions) {
        Type type = null;
        for (int i = 0; i < source.getExprCount(); ++i) {
            Expr expr = source.getExpr(i);
            if (!(expr instanceof KeyExpr)) {
                Type exprType = expr.getSelfType();
                if (type == null) {
                    type = exprType;
                } else {
                    for (TypeConversion conversion : conversions) {
                        Type conversionType = conversion.getType(type, exprType);
                        if (conversionType != null) {
                            type = conversionType;
                            break;
                        }
                    }
                }
            }
        }
        return type;
    }

    @Override
    public ConcreteClass getStaticClass(ExprSource source) {
        return (ConcreteClass) getCompatibleType(source, conversions);
    }

    @Override
    public Type getType(ExprSource source, KeyType keyType) {
        return getCompatibleType(source, conversions);
    }
}
