package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.AppImage;
import lsfusion.interop.form.event.BindingMode;

import java.util.Map;

public class InputListAction {
    public AppImage action;
    public String id;
    public AsyncEventExec asyncExec; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public String keyStroke;
    public Map<String, BindingMode> bindingModesMap;
    public Integer priority;
    public ImList<QuickAccess> quickAccessList;
    public int index;

    public InputListAction(AppImage action, String id, AsyncEventExec asyncExec, String keyStroke, Map<String, BindingMode> bindingModesMap, Integer priority, ImList<QuickAccess> quickAccessList, int index) {
        this.action = action;
        this.id = id;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.bindingModesMap = bindingModesMap;
        this.priority = priority;
        this.quickAccessList = quickAccessList;
        this.index = index;
    }
}