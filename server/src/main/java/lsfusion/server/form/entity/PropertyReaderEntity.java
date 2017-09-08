package lsfusion.server.form.entity;

import lsfusion.server.form.instance.PropertyType;
import lsfusion.server.profiler.ProfiledObject;

public interface PropertyReaderEntity extends ProfiledObject {

    byte getTypeID();

    int getID(); // ID в рамках Type

    PropertyType getPropertyType(FormEntity formEntity);
}
