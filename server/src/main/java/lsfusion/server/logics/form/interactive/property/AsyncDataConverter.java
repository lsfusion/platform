package lsfusion.server.logics.form.interactive.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public interface AsyncDataConverter<P extends PropertyInterface> {

    Object convert(ImMap<P, DataObject> values);
}
