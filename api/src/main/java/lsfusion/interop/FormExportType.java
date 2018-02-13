package lsfusion.interop;

public enum FormExportType implements FormStaticType {
    XML, JSON, CSV, DBF;

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
        }
        throw new UnsupportedOperationException();
    }

    public boolean isPlain() {
        return this == CSV || this == DBF;
    }
    
    
}
