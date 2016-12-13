package lsfusion.server.logics.property.actions.exporting.json;

import com.google.common.base.Throwables;
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
            JSONObject rootElement = new JSONOrderObject();
            JSONObject exportObject = new JSONOrderObject();
            JSONArray exportArray = new JSONArray();
            exportObject.put("group", exportArray);
            rootElement.put("export", exportObject);

            for (Node rootNode : rootNodes)
                exportNode(exportObject, rootNode);

            file = File.createTempFile("exportForm", ".json");
            try (PrintWriter out = new PrintWriter(file)) {
                out.println(rootElement.toString());
            }
            return IOUtils.getFileBytes(file);
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        } finally {
            if (file != null && !file.delete())
                file.deleteOnExit();
        }
    }

    private void exportNode(Object parentElement, AbstractNode node) throws JSONException {
        if(node instanceof Leaf) {
            ((JSONObject) parentElement).put(((Leaf) node).getKey(), ((Leaf) node).getValue());
        } else if(node instanceof Node) {
            for (Map.Entry<String, List<AbstractNode>> child : ((Node) node).getChildren()) {
                JSONArray array = new JSONArray();
                for(AbstractNode childNode : child.getValue()) {
                    if(childNode instanceof Leaf) {
                        exportNode(parentElement, childNode);
                    } else {
                        JSONObject object = new JSONOrderObject();
                        exportNode(object, childNode);
                        array.put(object);
                    }
                }
                if(array.length() > 0)
                    ((JSONObject) parentElement).put(child.getKey(), array);
            }
        }
    }
}