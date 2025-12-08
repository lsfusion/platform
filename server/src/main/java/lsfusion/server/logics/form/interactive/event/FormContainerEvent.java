package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.ComponentView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class FormContainerEvent extends FormServerEvent {
    public ComponentView container;
    public boolean collapse;

    public FormContainerEvent(ComponentView container, boolean collapse) {
        this.container = container;
        this.collapse = collapse;
    }

    @Override
    public FormServerEvent get(ObjectMapping mapping) {
        return new FormContainerEvent(mapping.get(container), collapse);
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