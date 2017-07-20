package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.PropertyInterfaceImplement;

public class Case<P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>, M extends PropertyInterfaceImplement<P>> {

    public final W where;
    public final M implement;

    public Case(W where, M implement) {
        this.where = where;
        this.implement = implement;
        assert implement != null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Case && implement.equalsMap(((Case) o).implement) && where.equalsMap(((Case) o).where);        
    }
    
    @Override
    public int hashCode() {
        return 31 * where.hashMap() + implement.hashMap();
    }

    @Override
    public String toString() {
        return where + " -> " + implement;
    }
}
