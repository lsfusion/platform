package lsfusion.server.data;

public abstract class SQLHandledException extends Exception{

    public boolean isRepeatableApply() {
        return true;
    }
}
