package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.exporting.json.JSONOrderObject;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ExportJSONDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {

    public ExportJSONDataActionProperty(LocalizedString caption, String extension,
                                        ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                        ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, CalcPropertyInterfaceImplement<I> where, LCP targetProp) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, where, targetProp);
    }

    @Override
    protected byte[] getFile(Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        File file = File.createTempFile("export", ".json");
        try {
            JSONObject rootElement = new JSONOrderObject();

            for (ImMap<String, Object> row : rows) {
                for (String key : row.keyIt()) {
                    Object cellValue = fieldTypes.getType(key).format(row.get(key));
                    if (cellValue != null) {
                        if (cellValue instanceof String)
                            rootElement.put(key, cellValue);
                        else if (cellValue instanceof byte[])
                            rootElement.put(key, Base64.encodeBase64String((byte[]) cellValue));
                    }
                }
            }

            try (PrintWriter out = new PrintWriter(file)) {
                out.println(rootElement.toString());
            }
            return IOUtils.getFileBytes(file);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        } finally {
            if (!file.delete())
                file.deleteOnExit();
        }
    }
}
