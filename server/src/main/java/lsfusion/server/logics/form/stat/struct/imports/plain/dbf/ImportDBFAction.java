package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;

public class ImportDBFAction extends ImportPlainAction<ImportDBFIterator> {

    private final PropertyInterface memoInterface;

    public ImportDBFAction(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean hasWhere) {
        super(paramsCount, groupFiles, formEntity, charset != null ? charset : ExternalUtils.defaultDBFCharset, hasWhere);

        int shift = groupFiles.size() + (hasWhere ? 1 : 0);
        memoInterface =  shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, String wheres, ExecutionContext<PropertyInterface> context) throws IOException {
        RawFileData memo = null;
        if(memoInterface != null) {
            FileData memoObject = (FileData) context.getKeyObject(memoInterface);
            memo = memoObject != null ? memoObject.getRawFile() : null;
        }
        return new ImportDBFIterator(fieldTypes, file, charset, wheres, memo);
    }
}