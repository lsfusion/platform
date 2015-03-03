package lsfusion.server.logics.property.actions.importing.xml;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xBaseJ.xBaseJException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportXMLDataActionProperty extends ImportDataActionProperty {
    public ImportXMLDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException {
        return new ImportXMLIterator(getTable(file));
    }
    
    private List<List<String>> getTable(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException {
        List<List<String>> result = new ArrayList<List<String>>();

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new ByteArrayInputStream(file));
        Element rootNode = document.getRootElement();
        List childrenList = rootNode.getChildren();
        
        List<Integer> columns = null;
        for (Object childNode : childrenList) {
            List<Attribute> attributes = ((Element)childNode).getAttributes();
            if (columns == null) {
                Map<String, Integer> mapping = new HashMap<String, Integer>();
                for (int i = 0; i < attributes.size(); i++) {
                    mapping.put(attributes.get(i).getName(), i);
                }
                columns = getSourceColumns(mapping);   
            }
            
            List<String> listRow = new ArrayList<String>();
            for (Integer column : columns) {
                if (column < attributes.size()) {
                    Attribute attribute = attributes.get(column);
                    listRow.add(attribute.getValue());
                }
            }
            result.add(listRow);

        }
        return result;
    }
}
