package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.AppServerImage;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

import java.util.Map;
import java.util.function.Predicate;

public class InputListAction {
    public AppServerImage.Reader action;
    public String id;
    public AsyncEventExec asyncExec; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public String keyStroke;
    public Map<String, BindingMode> bindingModesMap;
    public Integer priority;
    public ImList<QuickAccess> quickAccessList;
    public Predicate<SecurityPolicy> check;
    public int index;

    public InputListAction(AppServerImage.Reader action, String id, AsyncEventExec asyncExec, String keyStroke, Map<String, BindingMode> bindingModesMap,
                           Integer priority, ImList<QuickAccess> quickAccessList, Predicate<SecurityPolicy> check, int index) {
        this.action = action;
        this.id = id;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.bindingModesMap = bindingModesMap;
        this.priority = priority;
        this.quickAccessList = quickAccessList;
        this.check = check;
        this.index = index;
    }
}