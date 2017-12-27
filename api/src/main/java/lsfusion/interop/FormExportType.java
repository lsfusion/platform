package lsfusion.interop;

public enum FormExportType implements FormStaticType {
    XML, JSON, CSV, DBF, LIST, JDBC;

    public boolean isCustom() {
        return true;
    }

    @Override
    public String getExtension() {
        switch (this) {
            case XML:
                return "xml";
            case JSON:
                return "json";
            case CSV:
                return "csv";
            case DBF:
                return "dbf";
            case LIST:
            case JDBC:
                return "jdbc";
        }
        throw new UnsupportedOperationException();
    }

    public boolean isPlain() {
        return this == CSV || this == DBF || this == LIST || this == JDBC;
    }


}
