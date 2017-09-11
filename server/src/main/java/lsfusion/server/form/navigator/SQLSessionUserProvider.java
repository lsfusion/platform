package lsfusion.server.form.navigator;

public interface SQLSessionUserProvider {
    Integer getCurrentUser();
    LogInfo getLogInfo();
    Integer getCurrentComputer();
}
