package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.form.SetUserFilters;
import lsfusion.gwt.shared.view.changes.dto.GPropertyFilterDTO;
import lsfusion.interop.FilterType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SetUserFiltersHandler extends FormServerResponseActionHandler<SetUserFilters> {
    public SetUserFiltersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SetUserFilters action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        List<byte[]> filters = new ArrayList<>();
        for (GPropertyFilterDTO filter : action.filters) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream outStream = new DataOutputStream(byteStream);
            outStream.writeByte(FilterType.COMPARE);
            outStream.writeInt(filter.propertyID);
            outStream.writeBoolean(filter.columnKey != null);
            if(filter.columnKey != null)
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

        return getServerResponseResult(form, form.remoteForm.setUserFilters(action.requestIndex, defaultLastReceivedRequestIndex, filters.toArray(new byte[filters.size()][])));
    }
}
