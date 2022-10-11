package lsfusion.server.logics.navigator.controller.env;

import lsfusion.interop.connection.LocalePreferences;
import lsfusion.server.physics.admin.log.LogInfo;

public interface SQLSessionContextProvider {
    Long getCurrentUser();
    String getCurrentAuthToken();
    LogInfo getLogInfo();
    Long getCurrentComputer();
    Long getCurrentConnection();

    // when called from the external thread (process monitor)
    default Long getThreadCurrentUser() {
        return getCurrentUser();
    }
    default Long getThreadCurrentComputer() {
        return getCurrentComputer();
    }

    LocalePreferences getLocalePreferences();
}
