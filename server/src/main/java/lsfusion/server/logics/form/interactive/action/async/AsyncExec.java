package lsfusion.server.logics.form.interactive.action.async;

public abstract class AsyncExec extends AsyncEventExec {
    public abstract AsyncExec merge(AsyncExec asyncExec);
}