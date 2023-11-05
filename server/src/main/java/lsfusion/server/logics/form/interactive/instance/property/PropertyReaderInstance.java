package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.server.physics.admin.profiler.ProfiledObject;

public interface PropertyReaderInstance extends ProfiledObject {

    PropertyObjectInstance getReaderProperty();
    default PropertyObjectInstance getCellProperty() {
        return getReaderProperty();
    }
    default PropertyObjectInstance getGroupProperty() { // PIVOT
        return getReaderProperty();
    }

    byte getTypeID();

    int getID(); // ID в рамках Type

}
