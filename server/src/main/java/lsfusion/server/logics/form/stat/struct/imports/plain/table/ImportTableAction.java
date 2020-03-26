package lsfusion.server.logics.form.stat.struct.imports.plain.table;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;

public class ImportTableAction extends ImportPlainAction<ImportTableIterator> {

    public ImportTableAction(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean hasWhere) {
        super(paramsCount, groupFiles, formEntity, charset, hasWhere);
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, String wheres, ExecutionContext<PropertyInterface> context) throws IOException {
        return new ImportTableIterator(fieldTypes, file, wheres);
    }
}
