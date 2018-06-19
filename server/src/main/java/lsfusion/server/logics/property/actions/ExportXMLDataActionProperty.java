package lsfusion.server.logics.property.actions;

import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.*;
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ExportXMLDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {
    private boolean hasListOption;

    public ExportXMLDataActionProperty(LocalizedString caption, String extension,
                                       ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                       ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs,
                                       ImMap<String, Type> types, CalcPropertyInterfaceImplement<I> where,
                                       ImOrderMap<String, Boolean> orders, LCP targetProp, boolean hasListOption) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, types, where, orders, targetProp);
        this.hasListOption = hasListOption;
    }

    @Override
    protected byte[] getFile(Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        File file = File.createTempFile("export", ".xml");
        try {
            Element rootElement = new Element("export");

            if (hasListOption) {
                if (!rows.isEmpty()) {
                    ImMap<String, Object> row = rows.get(0);
                    exportRow(rootElement, row, fieldTypes);
                }
            } else {
                for (ImMap<String, Object> row : rows) {
                    Element rowElement = new Element("row");
                    exportRow(rowElement, row, fieldTypes);
                    rootElement.addContent(rowElement);
                }
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

    private void exportRow(Element parentElement, ImMap<String, Object> row, Type.Getter<String> fieldTypes) {
        for (String key : row.keyIt()) {
            String cellValue = fieldTypes.getType(key).formatString(row.get(key));
            if (cellValue != null) {
                Element element = new Element(key);
                element.addContent(cellValue);
                parentElement.addContent(element);
            }
        }
    }
}
