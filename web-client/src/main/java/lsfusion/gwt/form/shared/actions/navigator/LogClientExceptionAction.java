package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.VoidResult;
import net.customware.gwt.dispatch.shared.Action;

public class LogClientExceptionAction implements Action<VoidResult>, NavigatorAction {
    public String title;
    // Throwable не сериализуется. поэтому пересылаем строку 
    public String throwable;

    public LogClientExceptionAction() {
    }

    public LogClientExceptionAction(String title, String throwable) {
        this.title = title;
        this.throwable = throwable;
    }
}
