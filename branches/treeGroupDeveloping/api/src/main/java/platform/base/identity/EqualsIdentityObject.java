package platform.base.identity;

public class EqualsIdentityObject extends IdentityObject {

    @Override
    public boolean equals(Object o) {
        return this == o || getClass()==o.getClass() && ID == ((EqualsIdentityObject) o).ID;
    }

    @Override
    public int hashCode() {
        return ID;
    }
}
