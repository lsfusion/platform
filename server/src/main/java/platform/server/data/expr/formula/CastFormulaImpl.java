package platform.server.data.expr.formula;

import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class CastFormulaImpl implements FormulaImpl {
    private DataClass castClass;

    public CastFormulaImpl(DataClass castClass) {
        this.castClass = castClass;
    }

    @Override
    public String getSource(CompileSource compile, ExprSource source) {
        assert source.getExprCount() == 1;
        return castClass.getCast(source.getSource(0, compile), compile.syntax, compile.env, false);
    }

    @Override
    public ConcreteClass getStaticClass(ExprSource source) {
        return castClass;
    }

    @Override
    public Type getType(ExprSource source, KeyType keyType) {
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
}
