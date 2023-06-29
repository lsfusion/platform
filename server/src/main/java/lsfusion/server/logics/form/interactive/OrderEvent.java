package lsfusion.server.logics.form.interactive;

import lsfusion.base.BaseUtils;

import java.util.Objects;

public class OrderEvent {
    public String groupObject;
    
    public OrderEvent(String groupObject) {
        this.groupObject = groupObject;
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
