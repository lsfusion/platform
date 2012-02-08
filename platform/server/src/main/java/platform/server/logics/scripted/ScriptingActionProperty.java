package platform.server.logics.scripted;

import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ActionProperty;

public abstract class ScriptingActionProperty extends ActionProperty {
    protected BusinessLogics<?> BL;

    public ScriptingActionProperty(BusinessLogics<?> BL) {
        this(BL, BL.LM.genSID(), new ValueClass[]{});
    }

    public ScriptingActionProperty(BusinessLogics<?> BL, ValueClass... classes) {
        super(BL.LM.genSID(), classes);
        this.BL = BL;
    }

    public ScriptingActionProperty(BusinessLogics<?> BL, String sID, ValueClass... classes) {
        super(sID, classes);
        this.BL = BL;
    }

    public ScriptingActionProperty(BusinessLogics<?> BL, String sID, String caption, ValueClass... classes) {
        super(sID, caption, classes);
        this.BL = BL;
    }
}
