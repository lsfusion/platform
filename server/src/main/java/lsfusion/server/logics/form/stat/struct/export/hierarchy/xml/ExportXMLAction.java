package lsfusion.server.logics.form.stat.struct.export.hierarchy.xml;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.PrintWriter;

import static lsfusion.base.BaseUtils.nvl;

public class ExportXMLAction<O extends ObjectSelector> extends ExportHierarchicalAction<XMLNode, O> {
    
    public ExportXMLAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                           ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                           FormIntegrationType staticType, LP exportFile, Integer selectTop, String charset, ValueClass root, ValueClass tag) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFile, selectTop, charset, root, tag);
    }

    protected XMLNode createRootNode(String root, String tag) {
        String elementName = nvl(root != null ? root : getForm().getIntegrationSID(), "export");
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
