package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.client.controller.remote.action.form.SetUserFilters;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SetUserFiltersHandler extends BatchRequestHandler<SetUserFilters> {
    public SetUserFiltersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(SetUserFilters action) {
        Map<Integer, byte[][]> serializedFilters = new LinkedHashMap<>();
        for (Integer gid : action.filters.keySet()) {
            serializedFilters.put(gid, serializeFilters(action.filters.get(gid)));
        }
        return new BatchRequest.SetUserFilters(serializedFilters);
    }

    protected static byte[][] serializeFilters(List<GPropertyFilterDTO> filtersDTO) {

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
                BaseUtils.serializeObject(outStream, converter.convertOrCast(filter.filterValue.content));
                outStream.writeBoolean(filter.junction);
                filters.add(byteStream.toByteArray());
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return filters.toArray(new byte[filters.size()][]);
    }
}
