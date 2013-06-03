package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.formula.conversion.ConversionSource;
import lsfusion.server.data.expr.formula.conversion.TypeConversion;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public abstract class ArithmeticFormulaImpl extends AbstractFormulaImpl {
    protected final ConversionSource conversionSource;

    public ArithmeticFormulaImpl(TypeConversion conversion, ConversionSource conversionSource) {
        super(conversion);
        this.conversionSource = conversionSource;
    }

    @Override
    public String getSource(CompileSource compile, ExprSource source) {
        assert source.getExprCount() == 2;

        Type type1 = source.getType(0, compile.keyType);
        Type type2 = source.getType(1, compile.keyType);

        String src1 = source.getSource(0, compile);
        String src2 = source.getSource(1, compile);

        String sumSource = conversionSource.getSource(compile, type1, type2, src1, src2);

        if (sumSource == null) {
            throw new RuntimeException("Can't build " + getOperationName() + " expression");
        }

        return sumSource;
    }

    public abstract String getOperationName();
}
