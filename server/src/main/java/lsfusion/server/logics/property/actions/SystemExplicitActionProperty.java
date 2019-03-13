package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// usually are created from parser
public abstract class SystemExplicitActionProperty extends ExplicitActionProperty {


    protected SystemExplicitActionProperty() {
        super();
    }
    
    protected SystemExplicitActionProperty(LocalizedString caption) {
        super(caption, new ValueClass[]{});
    }
    
    protected SystemExplicitActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected SystemExplicitActionProperty(ImOrderSet interfaces) {
        super(interfaces);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    protected SystemExplicitActionProperty(LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
    }
}
