package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.BaseUtils;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.actions.form.SetUserFilters;
import platform.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;
import platform.gwt.form.shared.view.filter.GDataFilterValue;
import platform.gwt.form.shared.view.filter.GObjectFilterValue;
import platform.gwt.form.shared.view.filter.GPropertyFilterValue;
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
            outStream.writeByte(filter.filterValue.getTypeID());

            switch (filter.filterValue.getTypeID()) {
                case 0:
                    BaseUtils.serializeObject(outStream, ((GDataFilterValue) filter.filterValue).value);
                    break;
                case 1:
                    outStream.writeInt(((GObjectFilterValue) filter.filterValue).object.ID);
                    break;
                case 2:
                    outStream.writeInt(((GPropertyFilterValue) filter.filterValue).property.ID);
            }

            outStream.writeBoolean(filter.junction);
            filters.add(byteStream.toByteArray());
        }

        return getServerResponseResult(form, form.remoteForm.setUserFilters(action.requestIndex, filters.toArray(new byte[filters.size()][])));
    }
}
