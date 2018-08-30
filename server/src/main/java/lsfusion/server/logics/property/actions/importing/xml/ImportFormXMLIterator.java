package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportFormXMLIterator extends ImportFormIterator {
    private Set<String> attrs;
    private List<Object> children;
    private int i;

    public ImportFormXMLIterator(Pair<String, Object> keyValueRoot, Map<String, List<List<String>>> formObjectGroups,
                                 Map<String, List<List<String>>> formPropertyGroups, Set<String> attrs) {
        Element root = keyValueRoot.second instanceof Attribute ? null : (Element) keyValueRoot.second;
        this.attrs = attrs;
        this.children = new ArrayList<>();

        if (root != null) {
            for (Object child : ((Element) keyValueRoot.second).getChildren()) {
                List<Element> subChildren = getSubChildren(formObjectGroups, formPropertyGroups, child); //check formObjectGroups & formPropertyGroups
                if (subChildren != null) {
                    this.children.addAll(subChildren);
                } else {
                    if (notLeaf(child)) {
                        this.children.add(child);
                    }
                }
            }
            for (Object attribute : root.getAttributes()) {
                if (attribute instanceof Attribute && attrs.contains(((Attribute) attribute).getName())) {
                    children.add(attribute);
                }
            }
        }
        i = 0;
    }

    @Override
    public boolean hasNext() {
        return children != null && children.size() > i;
    }

    @Override
    public Pair<String, Object> next() {
        if (children != null) {
            Object child = children.get(i);
            Pair<String, Object> entry = Pair.create(child instanceof Attribute ? ((Attribute) child).getName() : ((Element) child).getName(), child);
            i++;
            return entry;
        } else
            return null;
    }

    @Override
    public void remove() {
    }

    private boolean notLeaf(Object child) {
        return !(child instanceof Element && ((Element) child).getChildren().isEmpty() && attrs.contains(((Element) child).getName()));
    }

    private List<Element> getSubChildren(Map<String, List<List<String>>> formObjectGroups, Map<String, List<List<String>>> formPropertyGroups, Object child) {
        //possible trouble if object and property has equal names and not equal groups
        List<Element> result = null;
        if (child instanceof Element) {
            List<List<String>> formObjectGroupList = formObjectGroups.get(((Element) child).getName());
            if (formObjectGroupList != null) {
                for (List<String> formObjectGroupEntry : formObjectGroupList) {
                    List<Element> subChildren = null;
                    //order is important
                    for (int i = 0; i < formObjectGroupEntry.size(); i++) {
                        List<Element> currentSubChildren = new ArrayList<>();
                        String subGroup = formObjectGroupEntry.get(i);
                        if (subChildren == null) {
                            currentSubChildren.addAll(getChildren((Element) child, subGroup));
                        } else {
                            for (Object subChild : subChildren) {
                                currentSubChildren.addAll(getChildren((Element) subChild, subGroup));
                            }
                        }
                        subChildren = currentSubChildren;
                    }
                    if (result == null)
                        result = new ArrayList<>();
                    if (subChildren != null)
                        result.addAll(subChildren);
                }
            } else {
                List<List<String>> formPropertyGroupList = formPropertyGroups.get(((Element) child).getName());
                if (formPropertyGroupList != null) {
                    for (List<String> formPropertyGroupEntry : formPropertyGroupList) {
                        List<Element> subChildren = null;
                        //order is important
                        for (int i = 0; i < formPropertyGroupEntry.size(); i++) {
                            List<Element> currentSubChildren = new ArrayList<>();
                            String subGroup = formPropertyGroupEntry.get(i);
                            if (subChildren == null) {
                                currentSubChildren.addAll(getChildren((Element) child, subGroup));
                            } else {
                                for (Object subChild : subChildren) {
                                    currentSubChildren.addAll(getChildren((Element) subChild, subGroup));
                                }
                            }
                            subChildren = currentSubChildren;
                        }
                        if (result == null)
                            result = new ArrayList<>();
                        if (subChildren != null)
                            result.addAll(subChildren);
                    }
                }
            }
        }
        return result;
    }

    private List<Element> getChildren(Element parent, String childName) {
        List<Element> result = new ArrayList<>();
        for (Object child : parent.getChildren()) {
            if (child instanceof Element && ((Element) child).getName().equals(childName) && notLeaf(child)) {
                result.add((Element) child);
            }
        }
        return result;
    }

}