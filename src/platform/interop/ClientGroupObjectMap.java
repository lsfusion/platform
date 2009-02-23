package platform.interop;

import java.util.LinkedHashMap;
import java.io.Serializable;

class ClientGroupObjectMap<T> extends LinkedHashMap<ClientObjectImplement,T>
                              implements Serializable {


/*  На самом деле не надо - так как сравнивать как раз надо именно по значениям
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    // Здесь по хорошему нужно hashcode когда новые свойства появятся перегрузить
    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    } */

}
