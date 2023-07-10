package lsfusion.server.logics.form.interactive;

import lsfusion.base.BaseUtils;
import lsfusion.server.language.property.LP;

import java.util.Objects;

public class OrderEvent {
    public String groupObject;
    public LP toProperty;
    
    public OrderEvent(String groupObject) {
        this(groupObject, null);
    }

    public OrderEvent(String groupObject, LP toProperty) {
        this.groupObject = groupObject;
        this.toProperty = toProperty;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OrderEvent) {
            return BaseUtils.nullEquals(groupObject, ((OrderEvent) obj).groupObject);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupObject);
    }
}
