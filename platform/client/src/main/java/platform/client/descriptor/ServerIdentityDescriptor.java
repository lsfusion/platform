package platform.client.descriptor;

public class ServerIdentityDescriptor extends IdentityDescriptor {

    @Override
    public boolean equals(Object o) {
        return this == o || getClass()==o.getClass() && ID == ((ServerIdentityDescriptor) o).ID;
    }

    @Override
    public int hashCode() {
        return ID;
    }
}
