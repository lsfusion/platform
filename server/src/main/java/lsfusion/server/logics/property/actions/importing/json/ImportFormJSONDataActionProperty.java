package lsfusion.server.logics.property.actions.importing.json;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportFormHierarchicalDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ImportFormJSONDataActionProperty extends ImportFormHierarchicalDataActionProperty<JSONObject> {

    public ImportFormJSONDataActionProperty(ValueClass[] classes, LCP<?> fileProperty, FormEntity formEntity, Map<String, List<List<String>>> formObjectGroups,
                                            Map<String, List<List<String>>> formPropertyGroups) {
        super(classes, fileProperty, formEntity, formObjectGroups, formPropertyGroups);
    }

    @Override
    public JSONObject getRootElement(byte[] file) {
        try {
            JSONObject json = JSONReader.read(file);
            Object rootNode = root == null ? json : JSONReader.findRootNode(json, null, root);
            if(rootNode instanceof JSONObject) {
                return (JSONObject) rootNode;
            } else if(rootNode instanceof JSONArray) {
                throw new RuntimeException(String.format("Import JSON error: root node %s should be object, not array", root));
            } else
                throw new RuntimeException(String.format("Import JSON error: root node %s not found", root));
        } catch (IOException | JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ImportFormIterator getIterator(Pair<String, Object> rootElement) {
        return new ImportFormJSONIterator(rootElement, formObjectGroups, formPropertyGroups);
    }

    @Override
    public String getChildValue(Object child) {
        return (String) child;
    }

    @Override
    public boolean isLeaf(Object child) {
        return !(child instanceof JSONArray || child instanceof JSONObject);
    }
}