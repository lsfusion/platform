package lsfusion.server.logics.property.actions.integration.hierarchy.xml;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.property.actions.integration.hierarchy.Node;
import org.jdom.Element;

import java.util.List;

public class XMLNode implements Node<XMLNode> {
    public final Element element; // addContent, getChildren

    public XMLNode(Element element) {
        this.element = element;
    }

    @Override
    public XMLNode getNode(String key) {
        return new XMLNode(element.getChild(key));
    }

    @Override
    public String getValue(String key, boolean attr) {
        if(attr)
            return element.getAttributeValue(key);
        return element.getChild(key).getText();
    }

    @Override
    public Iterable<Pair<Object, XMLNode>> getMap(String key, boolean isIndex) {
        MList<Pair<Object, XMLNode>> mResult = ListFact.mList();
        if(isIndex) {
            List children = element.getChildren(key);
            for (int i = 0; i < children.size(); i++)
                mResult.add(new Pair<Object, XMLNode>(i, new XMLNode((Element)children.get(i))));
        } else {
            Element child = element.getChild(key);
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

    public void addValue(XMLNode node, String key, boolean attr, String value) {
        if(attr) {
            node.element.setAttribute(key, value);
        } else {
            Element addElement = new Element(key);
            addElement.setText(value);

            node.element.addContent(addElement);
        }
    }

    public void addMap(XMLNode node, String key, boolean isIndex, Iterable<Pair<Object, XMLNode>> map) {
        if(isIndex) {
            for(Pair<Object, XMLNode> value : map) {
                value.second.element.setName(key);
                node.element.addContent(value.second.element);
            }
        } else {
            Element addElement = new Element(key);
            for(Pair<Object, XMLNode> value : map) {
                value.second.element.setName((String) value.first);                    
                addElement.addContent(value.second.element);
            }
            node.element.addContent(addElement);
        }
    }
}
