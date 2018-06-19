package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.exporting.json.JSONOrderObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ExportJSONDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {
    private boolean hasListOption;

    public ExportJSONDataActionProperty(LocalizedString caption, String extension,
                                        ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                        ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs,
                                        ImMap<String, Type> types, CalcPropertyInterfaceImplement<I> where,
                                        ImOrderMap<String, Boolean> orders, LCP targetProp, boolean hasListOption) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, types, where, orders, targetProp);
        this.hasListOption = hasListOption;
    }

    @Override
    protected byte[] getFile(Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        File file = File.createTempFile("export", ".json");
        try {
            if (hasListOption) {
                JSONObject rowElement = getRow(fieldTypes, rows.single());
                try (PrintWriter out = new PrintWriter(file, ExternalUtils.defaultXMLJSONCharset)) {
                    out.println(rowElement.toString());
                }
            } else {
                JSONArray rootElement = new JSONArray();
                for (ImMap<String, Object> row : rows) {
                    JSONObject rowElement = getRow(fieldTypes, row);
                    rootElement.put(rowElement);
                }
                try (PrintWriter out = new PrintWriter(file, ExternalUtils.defaultXMLJSONCharset)) {
                    out.println(rootElement.toString());
                }
            }
            return IOUtils.getFileBytes(file);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        } finally {
            if (!file.delete())
                file.deleteOnExit();
        }
    }

    private JSONObject getRow(Type.Getter<String> fieldTypes, ImMap<String, Object> row) throws JSONException {
        JSONObject rowElement = new JSONOrderObject();
        for (String key : row.keyIt()) {
            String cellValue = fieldTypes.getType(key).formatString(row.get(key));
            if (cellValue != null) {
                rowElement.put(key, cellValue);
            }
        }
        return rowElement;
    }
}
