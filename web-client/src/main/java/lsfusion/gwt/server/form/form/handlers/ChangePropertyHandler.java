package lsfusion.gwt.server.form.form.handlers;

import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.provider.FormSessionObject;
import lsfusion.gwt.server.form.convert.GwtToClientConverter;
import lsfusion.gwt.shared.form.actions.form.ChangeProperty;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;

import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends FormServerResponseActionHandler<ChangeProperty> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        Object value = gwtConverter.convertOrCast(action.value);
        byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);
        return getServerResponseResult(
                form,
                form.remoteForm.changeProperty(
                        action.requestIndex,
                        defaultLastReceivedRequestIndex,
                        action.propertyId,
                        fullKey,
                        serializeObject(gwtConverter.convertOrCast(value)),
                        action.addedObjectId
                )
        );
    }
}
