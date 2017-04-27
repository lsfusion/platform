package lsfusion.interop.form;

import lsfusion.interop.FormExportType;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.FormStaticType;

public enum ReportGenerationDataType {
    EXPORT, PRINTMESSAGE, PRINTJASPER;

    public static ReportGenerationDataType get(FormStaticType staticType) {
        return  staticType instanceof FormExportType ? EXPORT :
                staticType instanceof FormPrintType && staticType == FormPrintType.MESSAGE ? PRINTMESSAGE : PRINTJASPER;
    }

    public boolean isExport() {
        return this == EXPORT;
    }

    public boolean isPrintMessage() {
        return this == PRINTMESSAGE;
    }

    public boolean isPrintJasper() {
        return this == PRINTJASPER;
    }
}