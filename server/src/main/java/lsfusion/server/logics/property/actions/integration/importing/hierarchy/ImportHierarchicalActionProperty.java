package lsfusion.server.logics.property.actions.integration.importing.hierarchy;

import lsfusion.base.col.MapFact;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.hierarchy.Node;
import lsfusion.server.logics.property.actions.integration.hierarchy.ParseNode;
import lsfusion.server.logics.property.actions.integration.importing.FormImportData;
import lsfusion.server.logics.property.actions.integration.importing.ImportActionProperty;

import java.sql.SQLException;

public abstract class ImportHierarchicalActionProperty<T extends Node<T>> extends ImportActionProperty {

    private final PropertyInterface fileInterface;
    private final PropertyInterface rootInterface;

    public abstract T getRootNode(byte[] file, String root);

    public ImportHierarchicalActionProperty(int paramsCount, FormEntity formEntity) {
        super(paramsCount, formEntity);

        int shift = 0;
        this.fileInterface = getOrderInterfaces().get(shift++);
        rootInterface = shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    protected FormImportData getData(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String root = null;
        if(rootInterface != null)
            root = (String) context.getKeyObject(rootInterface);

        byte[] file = readFile(context.getKeyValue(fileInterface));

        ParseNode parseNode = formEntity.getImportHierarchy().getIntegrationHierarchy();
        FormImportData importData = new FormImportData(formEntity, context);
        if(file != null) {
            T rootNode = getRootNode(file, root);
            parseNode.importNode(rootNode, MapFact.<ObjectEntity, Object>EMPTY(), importData);
        }
        return importData;
    }

}