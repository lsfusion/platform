package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import static lsfusion.base.BaseUtils.nullEquals;

public class FormChangeEvent extends FormServerEvent {
    public PropertyDrawEntity propertyDrawEntity;
    public Boolean before;

    public FormChangeEvent(PropertyDrawEntity propertyDrawEntity, Boolean before) {
        this.propertyDrawEntity = propertyDrawEntity;
        this.before = before;
    }

    @Override
    public FormServerEvent get(ObjectMapping mapping) {
        return new FormChangeEvent(mapping.get(propertyDrawEntity), before);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("FormChangeEvent shoudn't be serialized");
    }

    public static FormChangeEvent deserialize(DataInputStream inStream) throws IOException {
        throw new UnsupportedOperationException("FormChangeEvent shoudn't be deserialized");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormChangeEvent that = (FormChangeEvent) o;
        return propertyDrawEntity.equals(that.propertyDrawEntity) && nullEquals(before, that.before);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyDrawEntity, before);
    }
}