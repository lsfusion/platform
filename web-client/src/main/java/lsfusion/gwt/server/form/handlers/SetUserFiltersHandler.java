package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.form.SetUserFilters;
import lsfusion.gwt.shared.view.changes.dto.GPropertyFilterDTO;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class SetUserFiltersHandler extends FormServerResponseActionHandler<SetUserFilters> {
    public SetUserFiltersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SetUserFilters action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        List<byte[]> filters = new ArrayList<>();
        try {
            for (GPropertyFilterDTO filter : action.filters) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream outStream = new DataOutputStream(byteStream);
                outStream.writeInt(filter.propertyID);
                outStream.writeBoolean(filter.columnKey != null);
                if (filter.columnKey != null) 
                    GwtToClientConverter.serializeGroupObjectValue(outStream, filter.columnKey);
                outStream.writeBoolean(filter.negation);
                outStream.writeByte(filter.compareByte);
                outStream.writeByte(filter.filterValue.typeID);

                GwtToClientConverter converter = GwtToClientConverter.getInstance();

                switch (filter.filterValue.typeID) {
                    case 0:
                        Object convertedValue = converter.convertOrCast(filter.filterValue.content);
                        BaseUtils.serializeObject(outStream, convertedValue);
                        break;
                    case 1:
                        outStream.writeInt((Integer) filter.filterValue.content);
                        break;
                    case 2:
                        outStream.writeInt((Integer) filter.filterValue.content);
                }

                outStream.writeBoolean(filter.junction);
                filters.add(byteStream.toByteArray());
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return getServerResponseResult(form, form.remoteForm.setUserFilters(action.requestIndex, defaultLastReceivedRequestIndex, filters.toArray(new byte[filters.size()][])));
    }
}
