package platform.server.logics.scheduler;

public interface SchedulerTask {

    String getID();
    void execute() throws Exception;
}
