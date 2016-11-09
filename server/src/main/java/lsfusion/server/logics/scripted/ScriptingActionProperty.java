package lsfusion.server.logics.scripted;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.debug.ActionDelegationType;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.UserActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public abstract class ScriptingActionProperty extends UserActionProperty {
    protected ScriptingLogicsModule LM;
    
    protected LCP<?> is(ValueClass valueClass) {
        return LM.is(valueClass);
    }

    protected LCP<?> object(ValueClass valueClass) {
        return LM.object(valueClass);
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM) {
        this(LM, new ValueClass[]{});
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(classes);
        this.LM = LM;
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, String caption, ValueClass... classes) {
        super(caption, classes);
        this.LM = LM;
    }

    protected LCP<?> findProperty(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findProperty(name);
    }

    protected LCP<?>[] findProperties(String... names) throws ScriptingErrorLog.SemanticErrorException {
        LCP<?>[] result = new LCP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
    }

    //этот метод нужен для дебаггера, чтобы была общая точка для дебаггинга всех executeCustom
    public void commonExecuteCustomDelegate(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        executeCustom(context);
    }

    protected LAP<?> findAction(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findAction(name);
    }

    protected ValueClass findClass(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findClass(name);
    }

    protected AbstractGroup findGroup(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findGroup(name);
    }

    protected FormEntity findForm(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findForm(name);
    }

    protected boolean applySession(ExecutionContext context, DataSession session) throws SQLException, SQLHandledException {
        return session.apply(context);
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE;
    }
}
