package lsfusion.server.logics.form.stat.struct;

import lsfusion.interop.form.print.FormStaticType;
import lsfusion.server.logics.classes.data.file.TableClass;

public enum FormIntegrationType implements FormStaticType {
    XLS, XLSX, DBF, CSV, XML, JSON, TABLE;

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
                return TableClass.extension;
        }
        throw new UnsupportedOperationException();
    }


    public boolean isPlain() {
        return !(this == XML || this == JSON);
    }
}