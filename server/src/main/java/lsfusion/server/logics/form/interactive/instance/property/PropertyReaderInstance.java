package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.server.physics.admin.profiler.ProfiledObject;

public interface PropertyReaderInstance extends ProfiledObject {

    PropertyObjectInstance getReaderProperty();

    byte getTypeID();

    int getID(); // ID в рамках Type

    // the property this reads (a part of), if it reads one - not a group's or a component's
    default PropertyDrawInstance<?> getPropertyDraw() {
        return null;
    }
}
