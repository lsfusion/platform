package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.shared.result.NumberResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.shared.actions.form.CountRecords;

import java.io.IOException;

public class CountRecordsHandler extends FormActionHandler<CountRecords, NumberResult> {
    public CountRecordsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CountRecords action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult(form.remoteForm.countRecords(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID));
    }
}
