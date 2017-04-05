package lsfusion.interop.form;

import lsfusion.interop.FormExportType;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.FormStaticType;

public enum ReportGenerationDataType {
    EXPORT, PRINT, DEFAULT;

    public static ReportGenerationDataType get(FormStaticType staticType) {
        return  staticType instanceof FormExportType ? EXPORT :
                staticType instanceof FormPrintType ? PRINT : DEFAULT;
    }

    public boolean isExport() {
        return this == EXPORT;
    }

    public boolean isPrint() {
        return this == PRINT;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }
}