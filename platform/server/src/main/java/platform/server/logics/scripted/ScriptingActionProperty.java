package platform.server.logics.scripted;

import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.logics.linear.LP;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;

import java.sql.SQLException;

public abstract class ScriptingActionProperty extends CustomActionProperty {
    protected ScriptingLogicsModule LM;

    public ScriptingActionProperty(ScriptingLogicsModule LM) {
        this(LM, new ValueClass[]{});
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        this(LM, LM.genSID(), classes);
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, String sID, ValueClass... classes) {
        super(sID, classes);
        this.LM = LM;
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, String sID, String caption, ValueClass... classes) {
        super(sID, caption, classes);
        this.LM = LM;
    }

    protected LP<?> getLP(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findLPByCompoundName(name);
    }

    protected ValueClass getClass(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findClassByCompoundName(name);
    }

    protected AbstractGroup getGroup(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findGroupByCompoundName(name);
    }

    protected FormEntity getForm(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findFormByCompoundName(name);
    }

    protected DataSession createSession() throws SQLException {
        return LM.getBL().createSession();
    }

    protected String applySession(DataSession session) throws SQLException {
        return session.apply(LM.getBL());
    }
}
