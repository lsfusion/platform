package lsfusion.server.logics.property.actions.exporting;

import com.google.common.base.Throwables;
import jasperapi.ClientReportData;
import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.interop.form.ReportConstants;
import lsfusion.interop.form.ReportGenerationData;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.*;

public abstract class FormExporter {
    Map<String, ClientReportData> data = null;
    Pair<String, Map<String, List<String>>> formHierarchy = null;

    public FormExporter(ReportGenerationData reportData) {
        try {
            this.data = ReportGenerator.retrieveReportSources(reportData, null).data;
            this.formHierarchy = ReportGenerator.retrieveReportHierarchy(reportData.reportHierarchyData);
        } catch (IOException | ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    FormObject createFormObject(String object, Map<String, List<String>> hierarchy) {
        FormObject fo = new FormObject(object);
        for (String dep : hierarchy.get(object)) {
            fo.dependencies.add(createFormObject(dep, hierarchy));
        }
        return fo;
    }

    Node createGroup(FormObject object) throws XMLStreamException {
        Node groupNode = new Node();
        Node node = new Node();
        for (HashMap<Integer, Object> keys : data.get(object.object).getKeyRows()) {
            node.addNode(object.object, createObjectValues(object, keys, new HashSet<String>()));
        }
        groupNode.addNode("group", node);
        return groupNode;
    }

    private AbstractNode createObjectValues(FormObject object, HashMap<Integer, Object> keys, Set<String> usedProperties) throws XMLStreamException {
        ClientReportData reportData = data.get(object.object);

        Node objectValueElement = new Node();

        for (Map.Entry<String, String> entry : getObjectElementsMap(reportData, keys, usedProperties).entrySet()) {
            objectValueElement.addLeaf(entry.getKey(), entry.getValue());
        }

        Set<String> usedPropertiesSet = new HashSet<>(usedProperties);
        usedPropertiesSet.addAll(reportData.getPropertyNames());

        for (FormObject dependent : object.dependencies) {
            ClientReportData dependentReportData = data.get(dependent.object);

            for (HashMap<Integer, Object> k : dependentReportData.getKeyRows()) {
                if (BaseUtils.containsAll(k, keys)) {
                    objectValueElement.addNode(dependent.object, createObjectValues(dependent, k, usedProperties));
                }
            }
        }
        return objectValueElement;
    }

    private Map<String, String> getObjectElementsMap(ClientReportData reportData, HashMap<Integer, Object> keys, Set<String> usedProperties) {
        Map<String, String> objectElementsMap = new HashMap<>();
        Map<Pair<Integer, Integer>, Object> values = reportData.getRows().get(keys);
        for (String property : reportData.getPropertyNames()) {
            String propertyTag = escapeTag(property);
            if (!usedProperties.contains(property) && !property.endsWith(ReportConstants.headerSuffix)) {
                if (reportData.getCompositeColumnObjects().containsKey(property)) {
                    for (List<Object> columnKeys : reportData.getCompositeColumnValues().get(property)) {
                        List<Integer> columnObjects = reportData.getCompositeColumnObjects().get(property);

                        List<Object> cKeys = new ArrayList<>();
                        for (Integer key : reportData.getCompositeFieldsObjects().get(property)) {
                            if (columnObjects.contains(key)) {
                                cKeys.add(columnKeys.get(columnObjects.indexOf(key)));
                            } else {
                                cKeys.add(keys.get(key));
                            }
                        }
                        objectElementsMap.put(reportData.getPropertyNames().contains(property + ReportConstants.headerSuffix) ?
                                String.valueOf(reportData.getCompositeObjectValues().get(property + ReportConstants.headerSuffix).get(cKeys)) :
                                propertyTag, String.valueOf(reportData.getCompositeObjectValues().get(property).get(cKeys)));
                    }
                } else {
                    if (filter(propertyTag))
                        objectElementsMap.put(propertyTag, String.valueOf(values.get(reportData.getProperties().get(property))));
                }
            }
        }
        return objectElementsMap;
    }


    private String escapeTag(String value) {
        return value.replace("_", "__").replace("()", "").replaceAll(",|\\(", "_").replace(")", "");
    }

    private boolean filter(String property) {
        return !property.contains("ADDOBJ") && !property.contains("action") && !property.contains("Action");
    }

    public class FormObject {
        public String object;
        public List<FormObject> dependencies = new ArrayList<>();

        FormObject(String obj) {
            object = obj;
        }
    }

    public abstract class AbstractNode {
    }

    public class Leaf extends AbstractNode {
        private String key;
        private String value;

        Leaf(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public class Node extends AbstractNode {
        private Map<String, List<AbstractNode>> valuesMap;

        public Node() {
            this.valuesMap = new LinkedHashMap<>();
        }

        public Set<Map.Entry<String, List<AbstractNode>>> getChildren() {
            return valuesMap.entrySet();
        }

        void addLeaf(String key, String value) {
            addNode(key, new Leaf(key, value));
        }

        void addNode(String key, AbstractNode value) {
            List<AbstractNode> nodeList = valuesMap.get(key);
            if (nodeList == null)
                nodeList = new ArrayList<>();
            nodeList.add(value);
            valuesMap.put(key, nodeList);
        }

    }
}