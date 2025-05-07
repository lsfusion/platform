package lsfusion.server.logics.form.stat.struct.imports.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.logics.form.stat.struct.hierarchy.json.JSONNode;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.ImportHierarchicalAction;
import lsfusion.server.logics.form.struct.FormEntity;
import org.json.JSONException;

import java.io.IOException;

public class ImportJSONAction extends ImportHierarchicalAction<JSONNode> {

    public ImportJSONAction(int paramsCount, FormEntity formEntity, String charset, boolean hasRoot, boolean hasWhere) {
        super(paramsCount, formEntity, charset, hasRoot, hasWhere);
    }

    @Override
    public JSONNode getRootNode(RawFileData fileData, String root) {
        try {
            return JSONNode.getJSONNode(JSONReader.readRootObject(fileData, root, getCharset()), true);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }
}