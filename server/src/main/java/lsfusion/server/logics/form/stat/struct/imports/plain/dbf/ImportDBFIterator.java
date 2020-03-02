package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;
import net.iryndin.jdbf.core.DbfField;

import java.io.IOException;
import java.util.Collection;

public class ImportDBFIterator extends ImportPlainIterator {
    private CustomDbfReader reader;
    private String charset;

    @Override
    protected boolean isFieldCI() {
        return true;
    }

    public ImportDBFIterator(ImOrderMap<String, Type> fieldTypes, RawFileData file, String charset, String wheres, RawFileData memo) throws IOException {
        super(fieldTypes, wheres);

        this.reader = new CustomDbfReader(file, memo);
        this.charset = charset;
        
        finalizeInit();
    }

    @Override
    protected ImOrderSet<String> readFields() {
        Collection<DbfField> fields = reader.getMetadata().getFields();
        MOrderExclSet<String> mResult = SetFact.mOrderExclSet(fields.size());         
        for(DbfField field : fields)
            mResult.exclAdd(field.getName());
        return mResult.immutableOrder();
    }
        
    private CustomDbfRecord record;
    @Override
    protected boolean nextRow(boolean checkWhere) throws IOException {
        do {
            record = reader.read();
            if (record == null) return false;
        } while (record.isDeleted() || (checkWhere && ignoreRow()));
        return true;        
    }

    @Override
    protected Object getPropValue(String name, Type type) throws ParseException, java.text.ParseException, IOException {
        return type.parseDBF(record, name, charset);
    }

    @Override
    protected Integer getRowIndex() {
        return record.getRecordNumber() - 1;
    }

    @Override
    public void release() {
        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}