package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetUserFilters;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
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
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                List<byte[]> filters = new ArrayList<>();
                try {
                    GwtToClientConverter converter = GwtToClientConverter.getInstance();

                    for (GPropertyFilterDTO filter : action.filters) {
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

                if (action.isViewFilter)
                    return remoteForm.setViewFilters(action.requestIndex, action.lastReceivedRequestIndex, filters.toArray(new byte[filters.size()][]));
                else
                    return remoteForm.setUserFilters(action.requestIndex, action.lastReceivedRequestIndex, filters.toArray(new byte[filters.size()][]));
            }
        });
    }
}
