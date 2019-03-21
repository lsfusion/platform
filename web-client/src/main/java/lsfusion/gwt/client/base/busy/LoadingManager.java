package lsfusion.gwt.client.base.busy;

public abstract class LoadingManager {
    public abstract void start();
    public abstract void stop();
    public abstract boolean isVisible();
}