package lsfusion.server.logics.property.actions.importing.dbf;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.SingleImportFormIterator;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class SingleDBFIterator extends SingleImportFormIterator {
    private DbfReader reader;
    private String charset;

    SingleDBFIterator(DbfReader reader, String charset) {
        this.reader = reader;
        this.charset = charset;
    }

    @Override
    public List<Pair<String, Object>> nextRow(String key) {
        try {
            DbfRecord record = reader.read();
            if (record == null)
                return null;

            List<Pair<String, Object>> listRow = new ArrayList<>();
            if (!record.isDeleted()) {
                for (DbfField field : record.getFields()) {
                    listRow.add(Pair.create(field.getName(), (Object) record.getString(field.getName(), charset)));
                }
                return listRow;
            }
            return listRow;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void release() {
        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}