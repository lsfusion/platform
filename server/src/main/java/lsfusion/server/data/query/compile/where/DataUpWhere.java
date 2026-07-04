package lsfusion.server.data.query.compile.where;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.JoinExprTranslator;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

public class DataUpWhere extends AbstractUpWhere<DataUpWhere> {

    private final DataWhere where;

    public DataUpWhere(DataWhere where) {
        this.where = where;
    }

    public int immutableHashCode() {
        return where.hashCode();
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return where.equals(((DataUpWhere)o).where);
    }

    @Override
    public Where getWhere(JoinExprTranslator translator) {
        return JoinExprTranslator.translateExpr(where, translator);
    }

    @Override
    public UpWhere translateExpr(ExprTranslator translator) {
        Where translated = where.translateExpr(translator);
        if(!(translated instanceof DataWhere))
            return null; // returning this would resurrect the original expression
        return new DataUpWhere((DataWhere) translated);
    }
}
