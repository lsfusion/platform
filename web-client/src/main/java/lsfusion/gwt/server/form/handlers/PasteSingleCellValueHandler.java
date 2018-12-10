package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.PasteSingleCellValue;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static lsfusion.base.BaseUtils.serializeObject;

public class PasteSingleCellValueHandler extends FormServerResponseActionHandler<PasteSingleCellValue> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public PasteSingleCellValueHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(PasteSingleCellValue action, ExecutionContext context) throws DispatchException, IOException {
        byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);

        byte[] value = serializeObject(
                gwtConverter.convertOrCast(action.value)
        );

        FormSessionObject form = getFormSessionObject(action.formSessionID);

        return getServerResponseResult(
                form,
                form.remoteForm.pasteMulticellValue(action.requestIndex, defaultLastReceivedRequestIndex, singletonMap(action.propertyId, singletonList(fullKey)), singletonMap(action.propertyId, value))
        );
    }
}
