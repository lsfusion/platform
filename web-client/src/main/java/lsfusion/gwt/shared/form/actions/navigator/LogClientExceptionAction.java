package lsfusion.gwt.shared.form.actions.navigator;

import com.google.gwt.core.shared.SerializableThrowable;
import lsfusion.gwt.shared.actions.VoidResult;

public class LogClientExceptionAction extends NavigatorAction<VoidResult> {
    public String title;
    public SerializableThrowable throwable;
    
    public boolean nonFatal = false;
    public int count = 0;
    public long reqId;

    public LogClientExceptionAction() {
    }

    public LogClientExceptionAction(String title, SerializableThrowable throwable) {
        this.title = title;
        this.throwable = throwable;
    }

    public LogClientExceptionAction(String title, SerializableThrowable throwable, int count, long reqId) {
        this(title, throwable);

        nonFatal = true;
        this.count = count;
        this.reqId = reqId;
    }

    @Override
    public boolean logRemoteException() {
        return false;
    }
}
