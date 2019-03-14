package lsfusion.server.logics.action;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// usually are created from parser
public abstract class SystemExplicitAction extends ExplicitAction {


    protected SystemExplicitAction() {
        super();
    }
    
    protected SystemExplicitAction(LocalizedString caption) {
        super(caption, new ValueClass[]{});
    }
    
    protected SystemExplicitAction(ValueClass... classes) {
        super(classes);
    }

    protected SystemExplicitAction(ImOrderSet interfaces) {
        super(interfaces);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    protected SystemExplicitAction(LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
    }
}
