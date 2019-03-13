package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.navigator.LogInfo;

public interface FocusListener {

    void gainedFocus(FormInstance form);

    LogInfo getLogInfo();    
}
