package lsfusion.server.logics.property.actions.integration.exporting.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.FormExportType;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.integration.hierarchy.json.JSONNode;
import lsfusion.server.logics.property.actions.integration.exporting.hierarchy.ExportHierarchicalActionProperty;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;

public class ExportJSONActionProperty<O extends ObjectSelector> extends ExportHierarchicalActionProperty<JSONNode, O> {

    public ExportJSONActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormExportType staticType, LCP exportFile, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFile, charset);
    }

    protected JSONNode createRootNode() {
        return new JSONNode(new JSONObject());
    }

    @Override
    protected void writeRootNode(PrintWriter printWriter, JSONNode rootNode) throws IOException {
        try {
            printWriter.println(JSONNode.putJSONNode(rootNode, true).toString());
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }
}
