package lsfusion.server.form.navigator;

public interface SQLSessionUserProvider {
    Long getCurrentUser();
    LogInfo getLogInfo();
    Long getCurrentComputer();
}
