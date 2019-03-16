package lsfusion.server.logics.navigator.controller.env;

import lsfusion.server.physics.admin.log.LogInfo;

public interface SQLSessionContextProvider {
    Long getCurrentUser();
    String getCurrentAuthToken();
    LogInfo getLogInfo();
    Long getCurrentComputer();
    Long getCurrentConnection();
}
