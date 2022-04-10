package lsfusion.server.logics.property.value;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class StaticValueProperty extends FormulaProperty<PropertyInterface> {

    public StaticValueProperty(LocalizedString caption, ImOrderSet<PropertyInterface> interfaces) {
        super(caption, interfaces);
    }

    public abstract Object getStaticValue();
}
