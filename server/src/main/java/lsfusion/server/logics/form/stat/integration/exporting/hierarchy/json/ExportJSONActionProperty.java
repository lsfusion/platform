package lsfusion.server.logics.form.stat.integration.exporting.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.exporting.hierarchy.ExportHierarchicalActionProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.form.stat.integration.hierarchy.json.JSONNode;
import lsfusion.server.logics.form.stat.integration.importing.hierarchy.json.JSONReader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;

public class ExportJSONActionProperty<O extends ObjectSelector> extends ExportHierarchicalActionProperty<JSONNode, O> {

    public ExportJSONActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                                    FormIntegrationType staticType, LCP exportFile, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFile, charset, null, null);
    }

    protected JSONNode createRootNode(String root, String tag) {
        return new JSONNode(new JSONObject());
    }

    @Override
    protected void writeRootNode(PrintWriter printWriter, JSONNode rootNode) throws IOException {
        try {
            JSONReader.writeRootObject(JSONNode.putJSONNode(rootNode, true), printWriter);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }
}
