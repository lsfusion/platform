package lsfusion.server.logics.form.stat.struct.export.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.ExportHierarchicalAction;
import lsfusion.server.logics.form.stat.struct.hierarchy.json.JSONNode;
import lsfusion.server.logics.form.stat.struct.hierarchy.json.OrderedJSONObject;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.json.JSONException;

import java.io.IOException;
import java.io.PrintWriter;

public class ExportJSONAction<O extends ObjectSelector> extends ExportHierarchicalAction<JSONNode, O> {

    public ExportJSONAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                            ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                            FormIntegrationType staticType, LP exportFile, Integer selectTop, String charset) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFile, selectTop, charset, null, null);
    }

    protected JSONNode createRootNode(String root, String tag) {
        return new JSONNode(new OrderedJSONObject());
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
