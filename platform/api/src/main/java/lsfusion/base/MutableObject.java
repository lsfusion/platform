package lsfusion.base;

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
