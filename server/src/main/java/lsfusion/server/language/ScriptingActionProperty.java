package lsfusion.server.language;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.ExplicitActionProperty;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.form.struct.group.AbstractGroup;

import java.sql.SQLException;

// !!! ONLY ACTIONS CREATED WITH INTERNAL OPERATOR !!!!
public abstract class ScriptingActionProperty extends ExplicitActionProperty {
    protected ScriptingLogicsModule LM;
    
    protected LCP<?> is(ValueClass valueClass) {
        return LM.is(valueClass);
    }

    protected LCP<?> object(ValueClass valueClass) {
        return LM.object(valueClass);
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(classes);
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

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // jump to java code
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    protected boolean allowNulls() { // does not allow by default
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange()) // неизвестно что поэтому считаем что изменяет
            return true;
        return super.hasFlow(type);
    }
}
