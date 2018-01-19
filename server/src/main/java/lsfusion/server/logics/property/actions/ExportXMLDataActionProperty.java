package lsfusion.server.logics.property.actions;

import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ExportXMLDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {

    public ExportXMLDataActionProperty(LocalizedString caption, String extension,
                                       ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                       ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, ImMap<String, Type> types, CalcPropertyInterfaceImplement<I> where, LCP targetProp) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, types, where, targetProp);
    }

    @Override
    protected byte[] getFile(Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        File file = File.createTempFile("export", ".xml");
        try {
            Element rootElement = new Element("export");

            for (ImMap<String, Object> row : rows) {
                Element rowElement = new Element("row");
                for (String key : row.keyIt()) {
                    String cellValue = fieldTypes.getType(key).formatString(row.get(key));
                    if (cellValue != null) {
                        Element element = new Element(key);
                        element.addContent(cellValue);
                        rowElement.addContent(element);
                    }
                }
                rootElement.addContent(rowElement);
            }

            XMLOutputter xmlOutput = new XMLOutputter(); // UTF-8 encoding
            xmlOutput.setFormat(Format.getPrettyFormat());
            try (PrintWriter fw = new PrintWriter(file, ExternalUtils.defaultXMLJSONCharset)) {
                xmlOutput.output(new Document(rootElement), fw);
            }
            return IOUtils.getFileBytes(file);
        } finally {
            if (!file.delete())
                file.deleteOnExit();
        }
    }
}
