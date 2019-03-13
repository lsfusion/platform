package lsfusion.server.logics.form.stat.integration.importing.plain.table;

import lsfusion.base.file.RawFileData;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainIterator;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.io.IOException;

public class ImportTableActionProperty extends ImportPlainActionProperty<ImportTableIterator> {

    public ImportTableActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset) {
        super(paramsCount, groupFiles, formEntity, charset);
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
        return new ImportTableIterator(fieldTypes, file);
    }
}
