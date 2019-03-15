package lsfusion.server.data.query.compile.where;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.translator.JoinExprTranslator;
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
        return JoinExprTranslator.translateExpr((Where)where, translator);
    }
}
