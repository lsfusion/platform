package lsfusion.interop.form.event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import static lsfusion.base.BaseUtils.nullEquals;

public class FormChangeEvent extends FormEvent {
    public Object propertyDrawEntity;
    public Boolean before;

    public FormChangeEvent(Object propertyDrawEntity, Boolean before) {
        this.propertyDrawEntity = propertyDrawEntity;
        this.before = before;
    }

    @Override
    public int getType() {
        return 3;
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