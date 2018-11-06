package lsfusion.server.logics.property.actions.integration.importing.plain.xls;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.Settings;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;

import java.io.IOException;

public class ImportXLSActionProperty extends ImportPlainActionProperty<ImportXLSIterator> {
    
    private final boolean sheetAll;
    private final PropertyInterface sheetInterface;

    public ImportXLSActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, boolean sheetAll) {
        super(paramsCount, groupFiles, formEntity);
        this.sheetAll = sheetAll;

        int shift = groupFiles.size();
        sheetInterface =  shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    public ImportPlainIterator getIterator(byte[] file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
        Integer singleSheetIndex = null;
        if(!sheetAll) {
            if(sheetInterface != null) {
                singleSheetIndex = (Integer) context.getKeyObject(sheetInterface);
                if(singleSheetIndex != null)
                    singleSheetIndex--; // one-based to zero-based
            }
            if(singleSheetIndex == null)
                singleSheetIndex = 0;
        }
        return new ImportXLSIterator(fieldTypes, file, file[0] == 80, singleSheetIndex);
    }
}
