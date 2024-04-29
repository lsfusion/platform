package lsfusion.interop.form.event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class FormContainerEvent extends FormEvent {
    public String container;
    public boolean collapse;

    public FormContainerEvent(String container, boolean collapse) {
        this.container = container;
        this.collapse = collapse;
    }

    @Override
    public int getType() {
        return 2;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("FormContainerEvent should not be serialized");
    }

    public static FormContainerEvent deserialize(DataInputStream inStream) throws IOException {
        throw new UnsupportedOperationException("FormContainerEvent should not be deserialized");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormContainerEvent that = (FormContainerEvent) o;
        return container.equals(that.container) && collapse == that.collapse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, collapse);
    }
}