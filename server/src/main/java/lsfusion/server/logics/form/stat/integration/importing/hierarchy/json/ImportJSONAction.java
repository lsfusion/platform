package lsfusion.server.logics.form.stat.integration.importing.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.logics.form.stat.integration.hierarchy.json.JSONNode;
import lsfusion.server.logics.form.stat.integration.importing.hierarchy.ImportHierarchicalAction;
import lsfusion.server.logics.form.struct.FormEntity;
import org.json.JSONException;

import java.io.IOException;

public class ImportJSONAction extends ImportHierarchicalAction<JSONNode> {

    public ImportJSONAction(int paramsCount, FormEntity formEntity, String charset) {
        super(paramsCount, formEntity, charset!= null ? charset : ExternalUtils.defaultXMLJSONCharset);
    }

    @Override
    public JSONNode getRootNode(RawFileData fileData, String root) {
        try {
            return JSONNode.getJSONNode(JSONReader.readRootObject(fileData, root, charset), true);
        } catch (IOException | JSONException e) {
            throw Throwables.propagate(e);
        }
    }
}