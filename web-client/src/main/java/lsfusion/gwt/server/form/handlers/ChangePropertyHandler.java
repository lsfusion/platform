package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.client.controller.remote.action.form.ChangeProperty;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends FormServerResponseActionHandler<ChangeProperty> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeProperty action, ExecutionContext context) {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        Object value = gwtConverter.convertOrCast(action.value);
        byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);
        try {
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
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
