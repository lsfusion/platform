package lsfusion.server.logics.property.actions.integration.importing.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.property.actions.integration.hierarchy.json.JSONNode;
import lsfusion.server.logics.property.actions.integration.importing.hierarchy.ImportHierarchicalActionProperty;
import org.json.JSONException;

import java.io.IOException;

public class ImportJSONActionProperty extends ImportHierarchicalActionProperty<JSONNode> {

    public ImportJSONActionProperty(int paramsCount, FormEntity formEntity) {
        super(paramsCount, formEntity);
    }

    @Override
    public JSONNode getRootNode(byte[] file, String root) {
        try {
            return JSONNode.getJSONNode(JSONReader.readRootObject(file, root), true);
        } catch (IOException | JSONException e) {
            throw Throwables.propagate(e);
        }
    }
}