package platform.server.data.expr.cases;

import platform.server.data.where.Where;

public abstract class Case<D> {
    public Where<?> where;
    public D data;

    public Case(Where iWhere,D iData) {
        where = iWhere;
        data = iData;
    }

    public String toString() {
        return where.toString() + "-" + data.toString();
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof Case && where.equals(((Case)obj).where) && data.equals(((Case)obj).data);
    }

    public int hashCode() {
        return where.hashCode()*31+data.hashCode();
    }
}
