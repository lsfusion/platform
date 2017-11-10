package lsfusion.server.integration;

import java.util.List;

/**
 * User: DAle
 * Date: 06.12.2010
 * Time: 14:26:36
 */

public class ImportTable extends PlainDataTable<ImportField> {
    public ImportTable(List<ImportField> fields, List<List<Object>> data) {
        super(fields, data);
    }
}
