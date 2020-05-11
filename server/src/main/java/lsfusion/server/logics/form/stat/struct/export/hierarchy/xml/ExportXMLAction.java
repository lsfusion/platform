package lsfusion.server.logics.form.stat.struct.export.hierarchy.xml;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.ExportHierarchicalAction;
import lsfusion.server.logics.form.stat.struct.hierarchy.xml.XMLNode;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.PrintWriter;

public class ExportXMLAction<O extends ObjectSelector> extends ExportHierarchicalAction<XMLNode, O> {
    
    public ExportXMLAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImList<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, LP exportFile, Integer selectTop, String charset, ValueClass root, ValueClass tag) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFile, selectTop, charset, root, tag);
    }

    protected XMLNode createRootNode(String root, String tag) {
        String elementName = root != null ? root : getForm().getIntegrationSID();
        Result<String> shortKey = new Result<>();
        Namespace namespace = XMLNode.addXMLNamespace(null, elementName, shortKey, false);
        return new XMLNode(new Element(shortKey.result, namespace), tag);
    }

    @Override
    protected void writeRootNode(PrintWriter printWriter, XMLNode rootNode) throws IOException {
        Element element = rootNode.element;
        XMLOutputter xmlOutput = new XMLOutputter() {
            @Override
            public String escapeElementEntities(String str) {
                return isXML(str) ? str : super.escapeElementEntities(str);
            }
        };
        xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(charset));
        xmlOutput.output(new Document(element), printWriter);
    }

    private boolean isXML(String str) {
        boolean isXML = false;
        if (str.startsWith("<") && str.endsWith(">") && str.contains("/")) {
            try {
                new SAXBuilder().build(IOUtils.toInputStream(str));
                isXML = true;
            } catch (JDOMException | IOException ignored) {
            }
        }
        return isXML;
    }
}
