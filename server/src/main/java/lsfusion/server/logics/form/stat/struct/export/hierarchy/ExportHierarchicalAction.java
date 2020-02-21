package lsfusion.server.logics.form.stat.struct.export.hierarchy;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.open.stat.ExportAction;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.StaticExportData;
import lsfusion.server.logics.form.stat.struct.hierarchy.Node;
import lsfusion.server.logics.form.stat.struct.hierarchy.ParseNode;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ExportHierarchicalAction<T extends Node<T>, O extends ObjectSelector> extends ExportAction<O> {

    private ClassPropertyInterface rootInterface;
    private ClassPropertyInterface tagInterface;

    protected final LP<?> exportFile; // nullable

    private static ValueClass[] getExtraParams(ValueClass root, ValueClass tag) {
        List<ValueClass> params = new ArrayList<>();
        if(root != null)
            params.add(root);
        if(tag != null)
            params.add(tag);
        return params.toArray(new ValueClass[params.size()]);
    }
    public ExportHierarchicalAction(LocalizedString caption,
                                    FormSelector<O> form,
                                    ImList<O> objectsToSet,
                                    ImList<Boolean> nulls,
                                    ImOrderSet<PropertyInterface> orderContextInterfaces,
                                    ImList<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                                    FormIntegrationType staticType,
                                    LP exportFile,
                                    Integer selectTop,
                                    String charset,
                                    ValueClass root, ValueClass tag) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, selectTop, charset != null ? charset : ExternalUtils.defaultXMLJSONCharset, getExtraParams(root, tag));

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        if (tag != null)
            this.tagInterface = orderInterfaces.get(orderInterfaces.size() - 1);
        if (root != null)
            this.rootInterface = orderInterfaces.get(orderInterfaces.size() - 1 - (tag != null? 1 : 0));

        this.exportFile = exportFile;
    }

    public void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException {
        ParseNode parseNode = hierarchy.getIntegrationHierarchy();
        String root = rootInterface == null ? null : (String) context.getKeyObject(rootInterface);
        String tag = tagInterface == null ? null : (String) context.getKeyObject(tagInterface);
        T rootNode = createRootNode(root, tag);
        parseNode.exportNode(rootNode, MapFact.EMPTY(), exportData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, charset)))) {
            writeRootNode(out, rootNode);
        }
        writeResult(exportFile, staticType, context, new RawFileData(outputStream));
    }

    protected abstract T createRootNode(String root, String tag);

    protected abstract void writeRootNode(PrintWriter printWriter, T rootNode) throws IOException;

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(exportFile.property);
    }
}
