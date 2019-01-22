package lsfusion.server.logics.property.actions.integration.importing.hierarchy;

import lsfusion.base.ExternalUtils;
import lsfusion.base.RawFileData;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.stat.StaticDataGenerator;
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

    public abstract T getRootNode(RawFileData fileData, String root);

    public ImportHierarchicalActionProperty(int paramsCount, FormEntity formEntity, String charset) {
        super(paramsCount, formEntity, charset);

        int shift = 0;
        this.fileInterface = getOrderInterfaces().get(shift++);
        rootInterface = shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    protected FormImportData getData(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String root = null;
        if(rootInterface != null)
            root = (String) context.getKeyObject(rootInterface);

        RawFileData file = readFile(context.getKeyValue(fileInterface));

        StaticDataGenerator.Hierarchy hierarchy = formEntity.getImportHierarchy();
        FormImportData importData = new FormImportData(formEntity, context);
        if(file != null) {
            T rootNode = getRootNode(file, root);
            hierarchy.getIntegrationHierarchy().importNode(rootNode, MapFact.<ObjectEntity, Object>EMPTY(), importData);
        }
        // filling properties that were not imported (to drop their changes too)
        for(ImOrderSet<PropertyDrawEntity> properties : hierarchy.getAllProperties())
            for(PropertyDrawEntity<?> property : properties)
                importData.addProperty(property.getImportProperty(), true); // isExclusive can be false, it doesn't matter        
        return importData;
    }

}