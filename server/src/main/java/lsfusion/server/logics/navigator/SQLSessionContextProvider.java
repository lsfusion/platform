package lsfusion.server.logics.navigator;

public interface SQLSessionContextProvider {
    Long getCurrentUser();
    String getCurrentAuthToken();
    LogInfo getLogInfo();
    Long getCurrentComputer();
    Long getCurrentConnection();
}
