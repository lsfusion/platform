package lsfusion.server.logics.form.stat.integration.exporting.hierarchy.xml;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.exporting.hierarchy.ExportHierarchicalActionProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.form.stat.integration.hierarchy.xml.XMLNode;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.PrintWriter;

public class ExportXMLActionProperty<O extends ObjectSelector> extends ExportHierarchicalActionProperty<XMLNode, O> {
    
    public ExportXMLActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                                   FormIntegrationType staticType, LCP exportFile, String charset, CalcProperty root, CalcProperty tag) {
        super(caption, form, objectsToSet, nulls, staticType, exportFile, charset, root, tag);
    }

    protected XMLNode createRootNode(String root, String tag) {
        String elementName = root != null ? root : form.getStaticForm().getIntegrationSID();
        Result<String> shortKey = new Result<>();
        Namespace namespace = XMLNode.addXMLNamespace(null, elementName, shortKey, false);
        return new XMLNode(new Element(shortKey.result, namespace), tag);
    }

    @Override
    protected void writeRootNode(PrintWriter printWriter, XMLNode rootNode) throws IOException {
        Element element = rootNode.element;
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(charset));
        xmlOutput.output(new Document(element), printWriter);
    }
}
