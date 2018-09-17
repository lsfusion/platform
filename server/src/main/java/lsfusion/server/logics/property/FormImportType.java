package lsfusion.server.logics.property;

public enum FormImportType {
    XLS, XLSX, DBF, CSV, XML, JSON, TABLE, MDB;

    public boolean isPlain() {
        return !(this == XML || this == JSON);
    }
}