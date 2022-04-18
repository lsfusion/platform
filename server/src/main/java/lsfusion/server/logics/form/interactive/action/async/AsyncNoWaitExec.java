package lsfusion.server.logics.form.interactive.action.async;

public class AsyncNoWaitExec extends AsyncExec {

    public AsyncNoWaitExec() {
    }

    public static AsyncNoWaitExec instance = new AsyncNoWaitExec();

    @Override
    public byte getTypeId() {
        return 5;
    }
}