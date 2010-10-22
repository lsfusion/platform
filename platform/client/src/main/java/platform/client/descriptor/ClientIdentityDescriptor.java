package platform.client.descriptor;

public class ClientIdentityDescriptor extends IdentityDescriptor {

    @Override
    public boolean equals(Object o) {
        return this == o || getClass()==o.getClass() && ID == ((ClientIdentityDescriptor) o).ID;
    }

    @Override
    public int hashCode() {
        return ID;
    }
}
