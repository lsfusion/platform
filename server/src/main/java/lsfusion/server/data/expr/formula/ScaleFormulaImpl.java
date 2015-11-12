package lsfusion.server.data.expr.formula;

import lsfusion.server.Settings;
import lsfusion.server.data.expr.formula.conversion.AbstractConversionSource;
import lsfusion.server.data.expr.formula.conversion.IntegralTypeConversion;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public abstract class ScaleFormulaImpl extends ArithmeticFormulaImpl {

    public ScaleFormulaImpl(ScaleConversionSource conversionSource) {
        super(IntegralTypeConversion.instance, conversionSource);
    }

    public static boolean isCastScale() {
        return Settings.get().getUseScaleOpType() == 1;
    }
    public static boolean isSafeCastScale() {
        return Settings.get().isUseSafeScaleCast();
    }

    protected static abstract class ScaleConversionSource extends AbstractConversionSource {

        public ScaleConversionSource() {
            super(IntegralTypeConversion.instance);
        }

        public String getScaleSource(String src, Type type, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString) {
            if(isToString || !isCastScale())
                return src;
            assert type != null;

            if(isSafeCastScale())
                return type.getSafeCast(src, syntax, env, null); // null так как не String
            else
                return type.getCast(src, syntax, env);
        }
    }

    public boolean hasNotNull() {
        return isCastScale() && isSafeCastScale();
    }
}
