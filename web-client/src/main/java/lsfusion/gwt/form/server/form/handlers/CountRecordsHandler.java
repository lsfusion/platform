package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.CountRecords;

import java.io.IOException;

public class CountRecordsHandler extends FormActionHandler<CountRecords, NumberResult> {
    public CountRecordsHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CountRecords action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult(form.remoteForm.countRecords(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID));
    }
}
