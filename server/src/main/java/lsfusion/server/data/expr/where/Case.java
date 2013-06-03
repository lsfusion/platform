package lsfusion.server.data.expr.where;

import lsfusion.server.data.where.Where;

public class Case<D> {
    public Where where;
    public D data;

    public Case(Where where,D data) {
        this.where = where;
        this.data = data;
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
