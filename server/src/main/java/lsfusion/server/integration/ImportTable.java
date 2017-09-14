package lsfusion.server.integration;

import java.util.List;

public class ImportTable extends PlainDataTable<ImportField> {
    public ImportTable(List<ImportField> fields, List<List<Object>> data) {
        super(fields, data);
    }
}
