package lsfusion.server.logics.property.actions.integration.exporting.hierarchy;

import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.FormExportType;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.integration.exporting.ExportActionProperty;
import lsfusion.server.logics.property.actions.integration.exporting.StaticExportData;
import lsfusion.server.logics.property.actions.integration.hierarchy.Node;
import lsfusion.server.logics.property.actions.integration.hierarchy.ParseNode;

import java.io.*;
import java.sql.SQLException;

public abstract class ExportHierarchicalActionProperty<T extends Node<T>, O extends ObjectSelector> extends ExportActionProperty<O> {

    protected final LCP<?> exportFile; // nullable

    protected final String charset; 
            
    public ExportHierarchicalActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormExportType staticType, LCP exportFile, String charset) {
        super(caption, form, objectsToSet, nulls, staticType);
        this.charset = charset == null ? ExternalUtils.defaultXMLJSONCharset : charset;
        
        this.exportFile = exportFile;
    }

    public void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException {
        ParseNode parseNode = hierarchy.getIntegrationHierarchy();
        T rootNode = createRootNode();
        parseNode.exportNode(rootNode, MapFact.<ObjectEntity, Object>EMPTY(), exportData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, charset)))) {
            writeRootNode(out, rootNode);
        }
        writeResult(exportFile, staticType, context, outputStream.toByteArray());
    }

    protected abstract T createRootNode();

    protected abstract void writeRootNode(PrintWriter printWriter, T rootNode) throws IOException;

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(exportFile.property);
    }
}
