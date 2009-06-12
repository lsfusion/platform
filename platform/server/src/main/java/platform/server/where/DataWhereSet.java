package platform.server.where;

import platform.base.QuickSet;

public class DataWhereSet extends QuickSet<DataWhere,DataWhereSet> {

    public DataWhereSet() {
    }

    public DataWhereSet(DataWhereSet set) {
        super(set);
    }

    public DataWhereSet(DataWhereSet[] sets) {
        super(sets);
    }

    protected DataWhere[] newArray(int size) {
        return new DataWhere[size];
    }

    protected DataWhereSet getThis() {
        return this;
    }
}

