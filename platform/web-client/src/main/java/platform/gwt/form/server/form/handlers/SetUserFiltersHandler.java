package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.BaseUtils;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.actions.form.SetUserFilters;
import platform.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;
import platform.interop.FilterType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SetUserFiltersHandler extends ServerResponseActionHandler<SetUserFilters> {
    public SetUserFiltersHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SetUserFilters action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        List<byte[]> filters = new ArrayList<byte[]>();
        for (GPropertyFilterDTO filter : action.filters) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream outStream = new DataOutputStream(byteStream);
            outStream.writeByte(FilterType.COMPARE);
            outStream.writeInt(filter.propertyID);
            outStream.writeBoolean(filter.negation);
            outStream.writeByte(filter.compareByte);
            outStream.writeByte(filter.filterValue.typeID);

            switch (filter.filterValue.typeID) {
                case 0:
                    BaseUtils.serializeObject(outStream, filter.filterValue.content);
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

        return getServerResponseResult(form, form.remoteForm.setUserFilters(action.requestIndex, filters.toArray(new byte[filters.size()][])));
    }
}
