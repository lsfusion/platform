package lsfusion.server.language.property.oraction;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public final class MappedActionOrProperty {
    public LAP<PropertyInterface, ?> property;
    public ImOrderSet<ObjectEntity> mapping;

    public MappedActionOrProperty(LAP property, ImOrderSet<ObjectEntity> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ActionOrProperty<PropertyInterface> getProperty() {
        return property.property;
    }
}
