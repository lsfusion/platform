package lsfusion.server.logics.form.stat.struct.imports.hierarchy;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.open.stat.ImportAction;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.hierarchy.Node;
import lsfusion.server.logics.form.stat.struct.imports.FormImportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public abstract class ImportHierarchicalAction<T extends Node<T>> extends ImportAction {

    private final PropertyInterface fileInterface;
    private final PropertyInterface rootInterface;
    protected final PropertyInterface whereInterface;

    public abstract T getRootNode(RawFileData fileData, String root);

    public ImportHierarchicalAction(int paramsCount, FormEntity formEntity, String charset, boolean hasRoot, boolean hasWhere) {
        super(paramsCount, formEntity, charset);

        int shift = 0;
        ImOrderSet<PropertyInterface> interfaces = getOrderInterfaces();
        this.fileInterface = interfaces.get(shift++);
        rootInterface = hasRoot ? interfaces.get(shift++) : null;
        whereInterface = hasWhere ? interfaces.get(shift) : null;
    }

    @Override
    protected FormImportData getData(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String root = rootInterface != null ? (String) context.getKeyObject(rootInterface) : null;
        String wheres = whereInterface != null ? (String) context.getKeyObject(whereInterface) : null;

        ImportHierarchicalIterator iterator = new ImportHierarchicalIterator(wheres);

        RawFileData file = readFile(context.getKeyValue(fileInterface));

        StaticDataGenerator.Hierarchy hierarchy = formEntity.getImportHierarchy();
        FormImportData importData = new FormImportData(formEntity, context);
        if(file != null && file.getLength() > 0) {
            T rootNode = getRootNode(file, root);
            hierarchy.getIntegrationHierarchy().importNode(rootNode, MapFact.EMPTY(), importData, iterator);
        }
        // filling properties that were not imported (to drop their changes too)
        for(ImOrderSet<PropertyDrawEntity> properties : hierarchy.getAllProperties())
            for(PropertyDrawEntity<?> property : properties)
                importData.addProperty(property, property.getImportProperty(), true); // isExclusive can be false, it doesn't matter
        return importData;
    }

}