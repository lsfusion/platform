package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.server.logics.form.interactive.MappingInterface;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerCustomSerializable;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntityOrPivotColumn;

public interface PropertyDrawViewOrPivotColumn<This extends PropertyDrawViewOrPivotColumn<This>> extends ServerCustomSerializable, MappingInterface<This> {

}