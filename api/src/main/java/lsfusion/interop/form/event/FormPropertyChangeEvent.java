package lsfusion.interop.form.event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class FormPropertyChangeEvent extends FormEvent {
    public Object propertyDrawEntity;

    public FormPropertyChangeEvent(Object propertyDrawEntity) {
        this.propertyDrawEntity = propertyDrawEntity;
    }

    @Override
    public int getType() {
        return 3;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("FormPropertyChangeEvent shoudn't be serialized");
    }

    public static FormPropertyChangeEvent deserialize(DataInputStream inStream) throws IOException {
        throw new UnsupportedOperationException("FormPropertyChangeEvent shoudn't be deserialized");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormPropertyChangeEvent that = (FormPropertyChangeEvent) o;
        return propertyDrawEntity.equals(that.propertyDrawEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyDrawEntity);
    }
}