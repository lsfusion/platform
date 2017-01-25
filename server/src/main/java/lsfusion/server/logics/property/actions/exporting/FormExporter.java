package lsfusion.server.logics.property.actions.exporting;

import com.google.common.base.Throwables;
import jasperapi.ClientReportData;
import jasperapi.ReportGenerator;
import jasperapi.ReportPropertyData;
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
            this.data = ReportGenerator.retrieveReportSources(reportData, null, true).data;
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
        ClientReportData dataEntry = data.get(object.object);
        for (HashMap<Integer, Object> keys : dataEntry.getKeyRows()) {
            node.addNode(object.object, createObjectValues(object, dataEntry, keys, new LinkedHashSet<String>()));
        }
        //данных нет, но создать пустой файл надо
        if(dataEntry.getKeyRows().isEmpty())
            node.addNode(object.object, createObjectValues(object, dataEntry, new HashMap<Integer, Object>(), new LinkedHashSet<String>()));
        groupNode.addNode("group", node);
        return groupNode;
    }

    private AbstractNode createObjectValues(FormObject object, ClientReportData dataEntry, HashMap<Integer, Object> keys, Set<String> usedProperties) throws XMLStreamException {
        ClientReportData reportData = dataEntry != null ? dataEntry : data.get(object.object);
        Node objectValueElement = new Node();
        if(reportData != null) {
            for (Map.Entry<Pair<String, ReportPropertyData>, Object> entry : getObjectElementsMap(reportData, keys, usedProperties).entrySet()) {
                objectValueElement.addLeaf(entry.getKey().first, entry.getKey().second, entry.getValue());
            }

            Set<String> usedPropertiesSet = new HashSet<>(usedProperties);
            usedPropertiesSet.addAll(reportData.getPropertyNames());

            for (FormObject dependent : object.dependencies) {
                ClientReportData dependentReportData = data.get(dependent.object);

                for (HashMap<Integer, Object> k : dependentReportData.getKeyRows()) {
                    if (BaseUtils.containsAll(k, keys)) {
                        objectValueElement.addNode(dependent.object, createObjectValues(dependent, null, k, usedProperties));
                    }
                }
                //данных нет, но создать пустой файл надо
                if(dependentReportData.getKeyRows().isEmpty())
                    objectValueElement.addNode(dependent.object, createObjectValues(dependent, null, new HashMap<Integer, Object>(), usedProperties));
            }
        }
        return objectValueElement;
    }

    private Map<Pair<String, ReportPropertyData>, Object> getObjectElementsMap(ClientReportData reportData, HashMap<Integer, Object> keys, Set<String> usedProperties) {
        Map<Pair<String, ReportPropertyData>, Object> objectElementsMap = new LinkedHashMap<>();
        Map<ReportPropertyData, Object> values = reportData.getRows().get(keys);
        Map<String, String> propertyTagMap = getPropertyTagMap(reportData.getPropertyNames());
        for (String property : reportData.getPropertyNames()) {
            String propertyTag = propertyTagMap.get(property);
            if (!usedProperties.contains(property) && !property.endsWith(ReportConstants.headerSuffix)) {
                ReportPropertyData propertyType = reportData.getProperties().get(property);
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
                        String propertyKey = reportData.getPropertyNames().contains(property + ReportConstants.headerSuffix) ?
                                String.valueOf(reportData.getCompositeObjectValues().get(property + ReportConstants.headerSuffix).get(cKeys)) :
                                propertyTag;
                        Object value = reportData.getCompositeObjectValues().get(property).get(cKeys);
                        objectElementsMap.put(Pair.create(propertyKey, propertyType), value);
                    }
                } else {
                    if (filter(propertyType))
                        objectElementsMap.put(Pair.create(propertyTag, propertyType), values == null ? null : values.get(reportData.getProperties().get(property)));
                }
            }
        }
        return objectElementsMap;
    }


    private String escapeTag(String value, boolean ignoreUnderscore) {
        return (ignoreUnderscore ? value : value.replace("_", "__")).replace("()", "").replaceAll(",|\\(", "_").replace(")", "");
    }

    private Map<String, String> getPropertyTagMap(List<String> propertyNames) {
        Map<String, String> propertyTagMap = new HashMap<>();
        boolean collision = false;
        for (String property : propertyNames) {
            String propertyTag = escapeTag(property, true);
            if (!propertyTagMap.containsKey(propertyTag))
                propertyTagMap.put(property, propertyTag);
            else {
                collision = true;
                break;
            }
        }
        if (collision) {
            propertyTagMap = new HashMap<>();
            for (String property : propertyNames) {
                propertyTagMap.put(property, escapeTag(property, false));
            }
        }
        return propertyTagMap;
    }

    private boolean filter(ReportPropertyData type) {
        return !type.propertyType.equals("ActionClass");
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
        private ReportPropertyData type;
        private Object value;

        Leaf(String key, ReportPropertyData type, Object value) {
            this.key = key;
            this.type = type;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public ReportPropertyData getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }

    public class Node extends AbstractNode {
        private Map<String, List<AbstractNode>> valuesMap;

        public Node() {
            this.valuesMap = new LinkedHashMap<>();
        }

        public List<Map.Entry<String, List<AbstractNode>>> getChildren() {
            return new ArrayList<>(valuesMap.entrySet());
        }

        void addLeaf(String key, ReportPropertyData type, Object value) {
            addNode(key, new Leaf(key, type, value));
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