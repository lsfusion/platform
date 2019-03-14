package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.physics.admin.logging.LogInfo;

public interface FocusListener {

    void gainedFocus(FormInstance form);

    LogInfo getLogInfo();    
}
