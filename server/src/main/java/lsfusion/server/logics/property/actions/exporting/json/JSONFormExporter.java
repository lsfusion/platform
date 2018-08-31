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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONFormExporter extends HierarchicalFormExporter {

    public JSONFormExporter(ReportGenerationData reportData, Map<String, List<String>> formObjectGroups, Map<String, List<String>> formPropertyGroups) {
        super(reportData, formObjectGroups, formPropertyGroups);
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
                JSONObject subParentElement = null;
                for(AbstractNode childNode : child.getValue()) {
                    subParentElement = null;
                    boolean isLeaf = childNode instanceof Leaf;

                    List<String> groups = isLeaf ? formPropertyGroups.get(child.getKey()) : formObjectGroups.get(child.getKey());
                    if(groups != null && parentElement instanceof JSONObject) {
                        for (int i = groups.size() - 1; i >= 0; i--) {
                            JSONObject subElement = findChild(subParentElement != null ? subParentElement : (JSONObject) parentElement, groups.get(i));
                            if (subElement == null) {
                                subElement = new JSONObject();
                                if (subParentElement == null) {
                                    ((JSONObject) parentElement).put(groups.get(i), subElement);
                                } else {
                                    subParentElement.put(groups.get(i), subElement);
                                }

                            }
                            subParentElement = subElement;
                        }
                    }

                    if (!isLeaf || ((Leaf) childNode).getType().toDraw.equals(parentId)) {
                        if (isLeaf) {
                            exportNode(subParentElement != null ? subParentElement : parentElement, child.getKey(), childNode);
                        } else if (child.getKey().equals(groupId)) {
                            exportNode(subParentElement != null ? subParentElement : parentElement, child.getKey(), childNode);
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
                        (subParentElement != null ? subParentElement : ((JSONObject) parentElement)).put(child.getKey(), array);
                    else if(parentElement instanceof JSONArray)
                        ((JSONArray) parentElement).put(array);
                }
            }
        }
    }

    private JSONObject findChild(JSONObject parent, String child) throws JSONException {
        JSONObject result = null;
        Iterator childIterator = parent.keys();
        while (childIterator.hasNext()) {
            String key = (String) childIterator.next();
            Object value = parent.get(key);
            if (value instanceof JSONObject && key.equals(child))
                result = (JSONObject) value;
        }
        return result;
    }
}