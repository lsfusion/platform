package lsfusion.server.form.instance.listener;

import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.navigator.LogInfo;

public interface FocusListener {

    void gainedFocus(FormInstance form);

    LogInfo getLogInfo();    
}
