package lsfusion.interop;


public enum FormPrintType {
    PRINT, AUTO, XLSX, XLS, PDF;
    
    public boolean isExcel() {
        return this == XLS || this == XLSX;
    }
    
    public String getFileExtension() {
        switch (this) {
            case XLS:
                return "xls";
            case XLSX:
                return "xlsx";
            default:
                return "pdf"; // по умолчанию экспортируем в PDF
        }
    }
}
