package platform.server.data.expr.formula;

import platform.server.data.expr.formula.conversion.IntegralTypeConversion;
import platform.server.data.expr.formula.conversion.StringTypeConversion;
import platform.server.data.expr.formula.conversion.TextTypeConversion;
import platform.server.data.expr.formula.conversion.TypeConversion;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class SumFormulaImpl extends AbstractFormulaImpl {
    public static TypeConversion sumConversions[] = {
            TextTypeConversion.instance,
            StringTypeConversion.instance,
            IntegralTypeConversion.instance
//            , DateTypeConversion.instance
    };

    public SumFormulaImpl() {
        super(sumConversions);
    }

    @Override
    public String getSource(CompileSource compile, ExprSource source) {
        assert source.getExprCount() == 2;

        Type type1 = source.getType(0, compile.keyType);
        Type type2 = source.getType(1, compile.keyType);

        String src1 = source.getSource(0, compile);
        String src2 = source.getSource(1, compile);

        String sumSource = null;
        for (TypeConversion conversion : conversions) {
            sumSource = conversion.getSource(compile, type1, type2, src1, src2);
            if (sumSource != null) {
                break;
            }
        }

        if (sumSource == null) {
            throw new RuntimeException("Can't build sum expression");
        }

        return sumSource;
    }
}
