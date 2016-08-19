package lsfusion.gwt.form.shared.actions.navigator;

import com.google.gwt.core.shared.SerializableThrowable;
import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.VoidResult;
import net.customware.gwt.dispatch.shared.Action;

public class LogClientExceptionAction implements Action<VoidResult>, NavigatorAction {
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
}
