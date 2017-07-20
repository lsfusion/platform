package lsfusion.server.data.expr.formula.conversion;

public abstract class AbstractConversionSource implements ConversionSource {

    protected final TypeConversion conversion;

    protected AbstractConversionSource(TypeConversion conversion) {
        this.conversion = conversion;
    }
}
