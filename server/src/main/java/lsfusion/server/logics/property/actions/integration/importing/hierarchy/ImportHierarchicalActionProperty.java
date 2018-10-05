package lsfusion.server.logics.property.actions.integration.importing.hierarchy;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.hierarchy.*;
import lsfusion.server.logics.property.actions.integration.importing.FormImportData;
import lsfusion.server.logics.property.actions.integration.importing.ImportActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public abstract class ImportHierarchicalActionProperty<T extends Node<T>> extends ImportActionProperty {

    private final PropertyInterface rootInterface;

    protected final LCP<?> fileProperty;
    protected final PropertyInterface fileInterface;

    public abstract T getRootNode(byte[] file, String root);

    public ImportHierarchicalActionProperty(int paramsCount, LCP<?> fileProperty, FormEntity formEntity) {
        super(paramsCount, formEntity);

        int shift = 0;        
        this.fileProperty = fileProperty;
        this.fileInterface = fileProperty == null ? getOrderInterfaces().get(shift++) : null;
        rootInterface = shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    protected FormImportData getData(ExecutionContext<PropertyInterface> context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        String root = null;
        if(rootInterface != null)
            root = (String) context.getKeyObject(rootInterface);

        byte[] file;
        if(fileProperty != null)
            file = readFile(fileProperty, (byte[]) fileProperty.read(context));
        else
            file = readFile(context.getKeyValue(fileInterface));

        ParseNode parseNode = formEntity.getImportHierarchy().getIntegrationHierarchy();
        FormImportData importData = new FormImportData(formEntity, context);
        if(file != null) {
            T rootNode = getRootNode(file, root);
            parseNode.importNode(rootNode, MapFact.<ObjectEntity, Object>EMPTY(), importData);
        }
        return importData;
    }

}