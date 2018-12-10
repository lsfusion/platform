package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.shared.result.NumberResult;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.shared.actions.form.CalculateSum;

import java.io.IOException;

public class CalculateSumHandler extends FormActionHandler<CalculateSum, NumberResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public CalculateSumHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CalculateSum action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult((Number) form.remoteForm.calculateSum(action.requestIndex, defaultLastReceivedRequestIndex, action.propertyID, (byte[]) gwtConverter.convertOrCast(action.columnKey)));
    }
}
