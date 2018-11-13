package lsfusion.server.logics.property.actions.integration.hierarchy.xml;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.hierarchy.Node;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;

public class XMLNode implements Node<XMLNode> {
    public final Element element; // addContent, getChildren
    private final String tag;

    public XMLNode(Element element) {
        this(element, null);
    }

    public XMLNode(Element element, String tag) {
        this.element = element;
        this.tag = tag;
    }

    @Override
    public XMLNode getNode(String key) {
        Element childElement = getXMLChild(key);
        if(childElement == null)
            return null;
        return new XMLNode(childElement);
    }
    
    private Namespace getXMLNamespace(String fullName, Result<String> shortName, boolean inheritNamespace) {
        int nsIndex = fullName.indexOf(":");
        if(nsIndex < 0) {
            shortName.set(fullName);
            
            Namespace defaultNamespace;
            if(inheritNamespace && (defaultNamespace = element.getNamespace("")) != null)
                return defaultNamespace;
            
            return Namespace.NO_NAMESPACE;
        }
        
        shortName.set(fullName.substring(nsIndex + 1));            
        return element.getNamespace(fullName.substring(0, nsIndex));
    }

    public String getXMLAttributeValue(String key) {
        Result<String> shortKey = new Result<>();
        Namespace namespace = getXMLNamespace(key, shortKey, false); // attributes don't inherit tags namespaces
        return element.getAttributeValue(shortKey.result, namespace);
    }

    public Element getXMLChild(String key) {
        Result<String> shortKey = new Result<>();
        Namespace namespace = getXMLNamespace(key, shortKey, true);
        return element.getChild(shortKey.result, namespace);
    }

    @Override
    public Object getValue(String key, boolean attr, Type type) throws ParseException {
        String stringValue;;
        if(attr)
            stringValue = getXMLAttributeValue(key);
        else {
            Element childElement = getXMLChild(key);
            stringValue = childElement != null ? childElement.getText() : null;
        }
        return type.parseXML(stringValue);
    }

    @Override
    public Iterable<Pair<Object, XMLNode>> getMap(String key, boolean isIndex) {
        MList<Pair<Object, XMLNode>> mResult = ListFact.mList();
        if(isIndex) {
            List children = key.equals("value") ? element.getChildren() : element.getChildren(key);
            for (int i = 0; i < children.size(); i++)
                mResult.add(new Pair<Object, XMLNode>(i, new XMLNode((Element)children.get(i))));
        } else {
            Element child = getXMLChild(key);
            if(child != null)
                for(Object value : child.getChildren())
                    mResult.add(new Pair<Object, XMLNode>(((Element)value).getName(), new XMLNode((Element)value)));
        }
        return mResult.immutableList();
    }

    public XMLNode createNode() {
        return new XMLNode(new Element("dumb"));
    }

    // because of the difference between edge and node-based approaches we have to set name while adding edges 
    public void addNode(XMLNode node, String key, XMLNode childNode) {
        childNode.element.setName(key);            
        node.element.addContent(childNode.element);
    }

    public void addValue(XMLNode node, String key, boolean attr, Object value, Type type) {
        String stringValue = type.formatXML(value);
        if(attr) {
            node.element.setAttribute(key, stringValue);
        } else {
            Element addElement = new Element(key);
            addElement.setText(stringValue);

            node.element.addContent(addElement);
        }
    }

    public void addMap(XMLNode node, String key, boolean isIndex, Iterable<Pair<Object, XMLNode>> map) {
        if(isIndex) {
            for(Pair<Object, XMLNode> value : map) {
                value.second.element.setName(tag != null ? tag : key);
                node.element.addContent(value.second.element);
            }
        } else {
            Element addElement = new Element(tag != null ? tag : key);
            for(Pair<Object, XMLNode> value : map) {
                value.second.element.setName((String) value.first);                    
                addElement.addContent(value.second.element);
            }
            node.element.addContent(addElement);
        }
    }
}
