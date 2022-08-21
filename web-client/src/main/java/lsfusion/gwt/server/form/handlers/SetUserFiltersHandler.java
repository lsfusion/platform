package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetUserFilters;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

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
    public ServerResponseResult executeEx(final SetUserFilters action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm ->
                remoteForm.setUserFilters(action.requestIndex, action.lastReceivedRequestIndex, serializeFilters(action.filters)));
    }

    protected static byte[][] serializeFilters(ArrayList<GPropertyFilterDTO> filtersDTO) {

        List<byte[]> filters = new ArrayList<>();
        try {
            GwtToClientConverter converter = GwtToClientConverter.getInstance();

            for (GPropertyFilterDTO filter : filtersDTO) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream outStream = new DataOutputStream(byteStream);
                outStream.writeInt(filter.propertyID);
                outStream.writeBoolean(filter.columnKey != null);
                if (filter.columnKey != null)
                    converter.serializeGroupObjectValue(outStream, filter.columnKey);
                outStream.writeBoolean(filter.negation);
                outStream.writeByte(filter.compareByte);
                outStream.writeByte(filter.filterValue.typeID);

                switch (filter.filterValue.typeID) {
                    case 0:
                        Object convertedValue = converter.convertOrCast(filter.filterValue.content);
                        BaseUtils.serializeObject(outStream, convertedValue);
                        break;
                    case 1:
                    case 2:
                        outStream.writeInt((Integer) filter.filterValue.content);
                        break;
                }

                outStream.writeBoolean(filter.junction);
                filters.add(byteStream.toByteArray());
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return filters.toArray(new byte[filters.size()][]);
    }
}
