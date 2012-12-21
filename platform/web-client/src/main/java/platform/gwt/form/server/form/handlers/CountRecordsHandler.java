package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.shared.actions.NumberResult;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.CountRecords;

import java.io.IOException;

public class CountRecordsHandler extends FormActionHandler<CountRecords, NumberResult> {
    public CountRecordsHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CountRecords action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult(form.remoteForm.countRecords(action.requestIndex, action.groupObjectID));
    }
}
