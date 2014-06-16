package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.type.Type;

public class CastFormulaImpl implements FormulaJoinImpl {
    private DataClass castClass;

    public CastFormulaImpl(DataClass castClass) {
        this.castClass = castClass;
    }

    @Override
    public String getSource(ExprSource source) {
        assert source.getExprCount() == 1;
        return castClass.getSafeCast(source.getSource(0), source.getSyntax(), source.getEnv(), source.getType(0));
    }

    @Override
    public Type getType(ExprType source) {
        return castClass;
    }

    @Override
    public int hashCode() {
        return castClass.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return castClass.equals(((CastFormulaImpl)obj).castClass);
    }

    public boolean hasNotNull() {
        return castClass.hasSafeCast();
    }
}
