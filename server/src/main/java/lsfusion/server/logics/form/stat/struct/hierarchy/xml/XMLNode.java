package lsfusion.server.logics.form.stat.struct.hierarchy.xml;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.hierarchy.Node;
import org.apache.commons.io.IOUtils;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
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

    private static String parseXMLNamespace(String fullName, Result<String> uri, Result<String> shortName) {
        int nsIndex = fullName.lastIndexOf(":");
        if(nsIndex < 0) {
            shortName.set(fullName);
            return null;
        }

        shortName.set(fullName.substring(nsIndex + 1));
        int uriIndex = fullName.indexOf("=");
        if(uriIndex < 0)
            return fullName.substring(0, nsIndex);
        
        uri.set(fullName.substring(uriIndex + 1, nsIndex));
        return fullName.substring(0, uriIndex);
    }
    
    private static Namespace getXMLNamespace(Element element, String fullName, Result<String> shortName, boolean inheritNamespace) {
        Result<String> uri = new Result<>();
        String nsName = parseXMLNamespace(fullName, uri, shortName);
        
        if(nsName == null) {
            shortName.set(fullName);
            
            Namespace defaultNamespace;
            if(inheritNamespace && (defaultNamespace = element.getNamespace("")) != null)
                return defaultNamespace;
            
            return Namespace.NO_NAMESPACE;
        }

        if(uri.result != null)
            return Namespace.getNamespace(nsName, uri.result);
        
        Namespace namespace = element != null ? element.getNamespace(nsName) : null;
        if(namespace == null)
            return Namespace.getNamespace(nsName,"http://www.w3.org/"+nsName);
        return namespace;
    }

    @Override
    public boolean isUpDown() {
        return true; // we need this to resolve namespaces in element.getNamespace
    }

    private Namespace getXMLNamespace(String fullName, Result<String> shortName, boolean inheritNamespace) {
        return getXMLNamespace(element, fullName, shortName, inheritNamespace);
    }

    public static Namespace addXMLNamespace(Element element, String fullName, Result<String> shortName, boolean inheritNamespace) {
        return getXMLNamespace(element, fullName, shortName, inheritNamespace);
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

    public List getXMLChildren(String key) {
        Result<String> shortKey = new Result<>();
        Namespace namespace = getXMLNamespace(key, shortKey, true);
        return element.getChildren(shortKey.result, namespace);
    }

    private static String getOutputFullText(Element content) {
        XMLOutputter outp = new XMLOutputter();
        outp.setFormat(Format.getRawFormat());
        StringWriter sw = new StringWriter();
        try {
            outp.output(content.getContent(), sw);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return sw.getBuffer().toString();
    }
    // copy of getText, but without Text filter
    private static String getFullText(Element element) {
        for(int i=0,size=element.getContentSize();i<size;i++) // optimization
            if(!(element.getContent(i) instanceof Text))
                return getOutputFullText(element);
        return element.getText();
    }

    @Override
    public Object getValue(String key, boolean attr, Type type) throws ParseException {
        String stringValue;
        if(attr)
            stringValue = getXMLAttributeValue(key);
        else {
            if(key.equals("value"))
                stringValue = element.getText(); // array and objects will be ignored (see getText implementation)
            else {
                Element childElement = getXMLChild(key);
                stringValue = childElement != null ? getFullText(childElement) : null; // array and objects will be ignored (see getText implementation)
            }
        }
        try {
            return type.parseXML(stringValue);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage() + String.format(" (tag %s)", key), e);
        }
    }

    @Override
    public Iterable<Pair<Object, XMLNode>> getMap(String key, boolean isIndex) {
        MList<Pair<Object, XMLNode>> mResult = ListFact.mList();
        if(isIndex) {
            List children = key.equals("value") ? element.getChildren() : getXMLChildren(key);
            for (int i = 0; i < children.size(); i++)
                mResult.add(new Pair<>(i, new XMLNode((Element) children.get(i))));
        } else {
            Element child = getXMLChild(key);
            if(child != null)
                for(Object value : child.getChildren())
                    mResult.add(new Pair<>(((Element) value).getName(), new XMLNode((Element) value)));
        }
        return mResult.immutableList();
    }

    public XMLNode createNode() {
        return new XMLNode(new Element("dumb"));
    }

    public void addXMLAttributeValue(Element element, String key, String stringValue) {
        if(key.toLowerCase().startsWith("xmlns")) {
            String nsName = "";
            int nsIndex = key.indexOf(":");
            if(nsIndex >= 0)
                nsName = key.substring(nsIndex + 1);

            Namespace namespace = Namespace.getNamespace(nsName, stringValue);
            element.addNamespaceDeclaration(namespace);
        } else {
            Result<String> shortKey = new Result<>();
            Namespace namespace = addXMLNamespace(element, key, shortKey, false);
            element.setAttribute(shortKey.result, stringValue, namespace);
        }
    }

    private static void addXMLChild(Element element, String key, String stringValue) {
        if(key.equals("value")) {
            element.addContent(stringValue);
        } else {
            Result<String> shortKey = new Result<>();
            Namespace namespace = addXMLNamespace(element, key, shortKey, true);
            Element addElement = new Element(shortKey.result, namespace);
            addElement.addContent(parseObject(stringValue));
            element.addContent(addElement);
        }
    }

    //check if it's XML inside
    private static List<Content> parseObject(String str) {
        if (str.matches(".*<.*/.*>.*")) {
            try {
                List<Content> children = new ArrayList<>(new SAXBuilder().build(IOUtils.toInputStream("<wrap>" + str + "</wrap>")).getRootElement().getContent());
                children.forEach(Content::detach);
                return children;
            } catch (JDOMException | IOException ignored) {
            }
        }
        return Collections.singletonList(new Text(str));
    }

    // because of the difference between edge and node-based approaches we have to set name while adding edges 
    private static void addXMLChild(Element element, String key, Element childElement) {
        Result<String> shortKey = new Result<>();
        Namespace namespace = addXMLNamespace(element, key, shortKey, true);
        childElement.setName(shortKey.result);
        childElement.setNamespace(namespace);
        element.addContent(childElement);
    }

    public void addNode(XMLNode node, String key, XMLNode childNode) {
        addXMLChild(node.element, key, childNode.element);
    }

    public void removeNode(XMLNode node, XMLNode childNode) {
        assert isUpDown();
        node.element.removeContent(childNode.element);
    }

    public void addValue(XMLNode node, String key, boolean attr, Object value, Type type) {
        String stringValue = type.formatXML(value);
        if(attr) {
            addXMLAttributeValue(node.element, key, stringValue);
        } else {
            addXMLChild(node.element, key, stringValue);
        }
    }

    public boolean addMap(XMLNode node, String key, boolean isIndex, Iterable<Pair<Object, XMLNode>> map) {
        boolean isNotEmpty = false;
        if(isIndex) {
            for(Pair<Object, XMLNode> value : map) {
                isNotEmpty = true;
                addXMLChild(node.element, tag != null ? tag : key, value.second.element);
            }
        } else {
            isNotEmpty = true;
            Element addElement = new Element(tag != null ? tag : key);
            for(Pair<Object, XMLNode> value : map) { // we don't support namespaces in getMap, so won't support it here
                value.second.element.setName((String) value.first);                    
                addElement.addContent(value.second.element);
            }
            node.element.addContent(addElement); // need to support namespaces, but it is not used for now
        }
        return isNotEmpty;
    }
}
