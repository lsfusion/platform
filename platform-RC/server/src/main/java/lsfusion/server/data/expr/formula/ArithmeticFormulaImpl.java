package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.formula.conversion.ConversionSource;
import lsfusion.server.data.expr.formula.conversion.TypeConversion;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;

public abstract class ArithmeticFormulaImpl extends AbstractFormulaImpl implements FormulaJoinImpl {
    protected final ConversionSource conversionSource;

    public ArithmeticFormulaImpl(TypeConversion conversion, ConversionSource conversionSource) {
        super(conversion);
        this.conversionSource = conversionSource;
    }

    
    private DataClass safeDataCast(Type type) {
        if(type instanceof ObjectType)
            return ObjectType.idClass;
        return (DataClass)type;
    }
    
    @Override
    public String getSource(ExprSource source) {
        assert source.getExprCount() == 2;

        DataClass type1 = safeDataCast(source.getType(0));
        DataClass type2 = safeDataCast(source.getType(1));

        String src1 = source.getSource(0);
        String src2 = source.getSource(1);

        String sumSource = conversionSource.getSource(type1, type2, src1, src2, source.getSyntax(), source.getMEnv(), source.isToString());

        if (sumSource == null) {
            throw new RuntimeException("Can't build " + getOperationName() + " expression");
        }

        return sumSource;
    }

    public abstract String getOperationName();

    public boolean hasNotNull() {
        return false;
    }
}
