package lsfusion.server.logics.property.actions.exporting.json;

import com.google.common.base.Throwables;
import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.HierarchicalFormExporter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class JSONFormExporter extends HierarchicalFormExporter {

    public JSONFormExporter(ReportGenerationData reportData) {
        super(reportData);
    }

    @Override
    public byte[] exportNodes(List<Node> rootNodes) throws IOException {
        File file = null;
        try {
            JSONObject rootObject = new JSONObject();

            for (Node rootNode : rootNodes)
                exportNode(rootObject, groupId, rootNode);

            file = File.createTempFile("exportForm", ".json");
            try (PrintWriter out = new PrintWriter(file, ExternalUtils.defaultXMLJSONCharset)) {
                out.println(rootObject.toString());
            }
            return IOUtils.getFileBytes(file);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        } finally {
            if (file != null && !file.delete())
                file.deleteOnExit();
        }
    }

    private void exportNode(Object parentElement, String parentId, AbstractNode node) throws JSONException {
        if (node instanceof Leaf) {
            String leafValue = getLeafValue((Leaf) node);
            if (leafValue != null) {
                ((JSONObject) parentElement).put(((Leaf) node).getKey(), leafValue);
            }
        } else if(node instanceof Node) {
            for (Map.Entry<String, List<AbstractNode>> child : ((Node) node).getChildren()) {
                JSONArray array = new JSONArray();
                for(AbstractNode childNode : child.getValue()) {
                    if (!(childNode instanceof Leaf) || ((Leaf) childNode).getType().toDraw.equals(parentId)) {
                        if (childNode instanceof Leaf) {
                            exportNode(parentElement, child.getKey(), childNode);
                        } else if (child.getKey().equals(groupId)) {
                            exportNode(parentElement, child.getKey(), childNode);
                        } else {
                            JSONObject object = new JSONOrderObject();
                            exportNode(object, child.getKey(), childNode);
                            if(object.length() > 0)
                                array.put(object);
                        }
                    }
                }
                if(array.length() > 0) {
                    if(parentElement instanceof JSONObject)
                        ((JSONObject) parentElement).put(child.getKey(), array);
                    else if(parentElement instanceof JSONArray)
                        ((JSONArray) parentElement).put(array);
                }
            }
        }
    }
}