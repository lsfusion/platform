package platform.base;

import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

// не должно быть equals и hashCode кроме identity
public class MutableObject {

    @Override
    public boolean equals(Object obj) {
        assert false;
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        assert false;
        return super.hashCode();
    }

}
