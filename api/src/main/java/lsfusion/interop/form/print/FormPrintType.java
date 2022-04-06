package lsfusion.interop.form.print;


public enum FormPrintType implements FormStaticType {
    PRINT, // preview - interactive
    MESSAGE, // message - interactive
    XLSX, XLS, PDF, DOC, DOCX, HTML, RTF; // external formats - static

    private static final String xlsPrefix = "xls_";

    public String getFormatPrefix() {
        return this == XLS || this == XLSX ? xlsPrefix : "";
    }

    public boolean ignorePagination() {
        return this == XLS || this == XLSX || this == HTML;
    }

    public String getExtension() {
        switch (this) {
            case XLS:
                return "xls";
            case XLSX:
                return "xlsx";
            case DOC:
                return "doc";
            case DOCX:
                return "docx";
            case RTF:
                return "rtf";
            case HTML:
                return "html";
            default:
                return "pdf"; // by default exporting to pdf
        }
    }
}
