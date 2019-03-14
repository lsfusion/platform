package lsfusion.server.logics.form.stat.integration.importing.hierarchy;

import lsfusion.base.file.RawFileData;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.integration.hierarchy.Node;
import lsfusion.server.logics.form.stat.integration.importing.FormImportData;
import lsfusion.server.logics.form.open.stat.ImportActionProperty;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

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