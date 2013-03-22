package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.server.form.entity.FormEntity;
import platform.server.form.instance.FormInstance;
import platform.server.session.DataSession;

public class SessionEnvEvent implements FunctionSet<DataSession> {

    public final static SessionEnvEvent ALWAYS = new SessionEnvEvent(null);

    public final static SimpleAddValue<Object, SessionEnvEvent> mergeEnv = new SimpleAddValue<Object, SessionEnvEvent>() {
        public SessionEnvEvent addValue(Object key, SessionEnvEvent prevValue, SessionEnvEvent newValue) {
            return prevValue.merge(newValue);
        }

        public boolean symmetric() {
            return true;
        }
    };

    public static <T> AddValue<T, SessionEnvEvent> mergeSessionEnv() {
        return BaseUtils.immutableCast(mergeEnv);
    }

    public SessionEnvEvent merge(SessionEnvEvent env) {
        if(forms==null)
            return this;
        if(env.forms==null)
            return env;

        return new SessionEnvEvent(BaseUtils.merge(forms, env.forms));
    }

    private final FunctionSet<FormEntity> forms;
    public SessionEnvEvent(FunctionSet<FormEntity> forms) {
        this.forms = forms;
    }

    public boolean contains(DataSession element) {
        if(forms==null) { // вообще говоря не только оптимизация, так как activeForms может быть пустым
            assert this==ALWAYS;
            return true;
        }

        for(FormInstance form : element.getActiveForms())
            if(forms.contains(form.entity))
                return true;
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isFull() {
        return false;
    }
}
