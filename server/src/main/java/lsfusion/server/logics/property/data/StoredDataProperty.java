package lsfusion.server.logics.property.data;

import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class StoredDataProperty extends DataProperty {

    public StoredDataProperty(LocalizedString caption, ValueClass[] classes, ValueClass value) {
        super(caption, classes, value);

        finalizeInit();
    }

    public boolean isStored() {
        return true;
    }

    // нет
    public static SFunctionSet<Property> set = element -> element instanceof StoredDataProperty;

}
