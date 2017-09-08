package lsfusion.server.form.instance;

import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.profiler.ProfiledObject;

public interface PropertyReaderInstance extends ProfiledObject {

    CalcPropertyObjectInstance getPropertyObjectInstance();

    byte getTypeID();

    int getID(); // ID в рамках Type

    PropertyType getPropertyType(FormEntity formEntity);
}
