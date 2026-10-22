package lsfusion.server.logics.form.stat.struct.imports.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.logics.form.stat.struct.hierarchy.FormParseNode;
import lsfusion.server.logics.form.stat.struct.hierarchy.json.JSONNode;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.ImportHierarchicalAction;
import lsfusion.server.logics.form.struct.FormEntity;
import org.json.JSONException;

public class ImportJSONAction extends ImportHierarchicalAction<JSONNode> {

    public ImportJSONAction(int paramsCount, FormEntity formEntity, boolean hasRoot, boolean hasWhere, boolean hasCharset) {
        super(paramsCount, formEntity, ExternalUtils.defaultXMLJSONCharset, hasRoot, hasWhere, hasCharset);
    }

    @Override
    public JSONNode getRootNode(RawFileData fileData, String root, String charset, FormParseNode formParseNode) {
        try {
            return JSONNode.getJSONNode(JSONReader.readRootObject(fileData, root, charset), true);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }
}