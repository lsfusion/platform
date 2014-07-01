package lsfusion.server.logics.scripted;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.UserActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public abstract class ScriptingActionProperty extends UserActionProperty {
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

    protected LP<?, ?> getLP(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findLPByCompoundOldName(name);
    }

    protected LCP<?> getLCP(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LCP<?>) getLP(name);
    }

    protected LCP<?>[] getLCPs(String... names) throws ScriptingErrorLog.SemanticErrorException {
        LCP<?>[] result = new LCP[names.length]
        for (int i = 0; i < names.length; i++) {
            result[i] = getLCP(names[i]);
        }
        return result;
    }

    protected LAP<?> getLAP(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LAP <?>) getLP(name);
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

    protected boolean applySession(ExecutionContext context, DataSession session) throws SQLException, SQLHandledException {
        return session.apply(context);
    }
}
