package lsfusion.server.logics.property.data;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.physics.admin.Settings;
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

    @Override
    public boolean isNameValueUnique() {
        return true;
    }
}
