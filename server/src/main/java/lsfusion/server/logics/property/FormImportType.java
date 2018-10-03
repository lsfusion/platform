package lsfusion.server.logics.property;

public enum FormImportType {
    XLS, XLSX, DBF, CSV, XML, JSON, TABLE;//, MDB;

    public String getExtension() {
        switch (this) {
            case XML:
                return "xml";
            case XLS:
                return "xls";
            case XLSX:
                return "xlsx";
            case JSON:
                return "json";
            case CSV:
                return "csv";
            case DBF:
                return "dbf";
            case TABLE:
                return "jdbc";
        }
        throw new UnsupportedOperationException();
    }


    public boolean isPlain() {
        return !(this == XML || this == JSON);
    }
}