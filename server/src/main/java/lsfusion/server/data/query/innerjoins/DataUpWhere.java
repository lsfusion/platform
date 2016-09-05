package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
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
    public Where getWhere() {
        return where;
    }
}
