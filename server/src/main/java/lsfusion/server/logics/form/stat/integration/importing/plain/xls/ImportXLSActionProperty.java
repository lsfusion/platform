package lsfusion.server.logics.form.stat.integration.importing.plain.xls;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainIterator;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;

public class ImportXLSActionProperty extends ImportPlainActionProperty<ImportXLSIterator> {

    private boolean noHeader;
    private final boolean sheetAll;
    private final PropertyInterface sheetInterface;

    public ImportXLSActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean noHeader, boolean sheetAll) {
        super(paramsCount, groupFiles, formEntity, charset);
        this.noHeader = noHeader;
        this.sheetAll = sheetAll;

        int shift = groupFiles.size();
        sheetInterface =  shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
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
        return new ImportXLSIterator(fieldTypes, file, file.getBytes()[0] == 80, noHeader, singleSheetIndex);
    }
}
