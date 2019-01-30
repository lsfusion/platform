package lsfusion.server.form.navigator;

public interface SQLSessionContextProvider {
    Long getCurrentUser();
    LogInfo getLogInfo();
    Long getCurrentComputer();
}
