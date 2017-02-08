package lsfusion.server.logics.property.actions.importing.json;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.property.actions.importing.ImportFormHierarchicalDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ImportFormJSONDataActionProperty extends ImportFormHierarchicalDataActionProperty<JSONObject> {

    public ImportFormJSONDataActionProperty(FormEntity formEntity) {
        super(new ValueClass[]{}, formEntity);
    }

    @Override
    public JSONObject getRootElement(byte[] file) {
        try {
            return JSONReader.read(file);
        } catch (IOException | JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ImportFormIterator getIterator(Pair<String, Object> rootElement) {
        return new ImportFormJSONIterator(rootElement);
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