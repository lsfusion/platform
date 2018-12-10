package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.CalculateSum;

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
