package platform.server.logics.scripted;

import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.actions.CustomActionProperty;

public abstract class ScriptingActionProperty<T extends BusinessLogics<T>> extends CustomActionProperty {
    protected T BL;

    public ScriptingActionProperty(T BL) {
        this(BL, BL.LM.genSID(), new ValueClass[]{});
    }

    public ScriptingActionProperty(T BL, ValueClass... classes) {
        super(BL.LM.genSID(), classes);
        this.BL = BL;
    }

    public ScriptingActionProperty(T BL, String sID, ValueClass... classes) {
        super(sID, classes);
        this.BL = BL;
    }

    public ScriptingActionProperty(T BL, String sID, String caption, ValueClass... classes) {
        super(sID, caption, classes);
        this.BL = BL;
    }
}
