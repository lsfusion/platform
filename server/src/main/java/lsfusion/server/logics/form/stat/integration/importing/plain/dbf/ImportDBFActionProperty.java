package lsfusion.server.logics.form.stat.integration.importing.plain.dbf;

import lsfusion.interop.session.ExternalUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainIterator;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainActionProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportDBFActionProperty extends ImportPlainActionProperty<ImportDBFIterator> {

    private final PropertyInterface whereInterface;
    private final PropertyInterface memoInterface;

    public ImportDBFActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean hasWhere) {
        super(paramsCount, groupFiles, formEntity, charset != null ? charset : ExternalUtils.defaultDBFCharset);

        int shift = groupFiles.size();
        whereInterface = hasWhere ? getOrderInterfaces().get(shift++) : null;
        memoInterface =  shift < paramsCount ? getOrderInterfaces().get(shift) : null;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
        String wheres = null;
        if(whereInterface != null)
            wheres = (String)context.getKeyObject(whereInterface);
        RawFileData memo = null;
        if(memoInterface != null) {
            FileData memoObject = (FileData) context.getKeyObject(memoInterface);
            memo = memoObject != null ? memoObject.getRawFile() : null;
        }
        return new ImportDBFIterator(fieldTypes, file, charset, memo, getWheresList(wheres));
    }

    private static List<List<String>> getWheresList(String wheres) {
        List<List<String>> wheresList = new ArrayList<>();
        if (wheres != null) { //spaces in value are not permitted
            Pattern wherePattern = Pattern.compile("(?:\\s(AND|OR)\\s)?(?:(NOT)\\s)?([^=<>\\s]+)(\\sIN\\s|=|<|>|<=|>=)([^=<>\\s]+)");
            Matcher whereMatcher = wherePattern.matcher(wheres);
            while (whereMatcher.find()) {
                String condition = whereMatcher.group(1);
                String not = whereMatcher.group(2);
                String field = whereMatcher.group(3);
                String sign = whereMatcher.group(4);
                String value = whereMatcher.group(5);
                wheresList.add(Arrays.asList(condition, not, field, sign, value));
            }
        }
        return wheresList;
    }
}