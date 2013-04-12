package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.shared.actions.NumberResult;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.convert.GwtToClientConverter;
import platform.gwt.form.shared.actions.form.CalculateSum;

import java.io.IOException;

public class CalculateSumHandler extends FormActionHandler<CalculateSum, NumberResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public CalculateSumHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CalculateSum action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult((Number) form.remoteForm.calculateSum(action.requestIndex, action.propertyID, (byte[]) gwtConverter.convertOrCast(action.columnKey)));
    }
}
