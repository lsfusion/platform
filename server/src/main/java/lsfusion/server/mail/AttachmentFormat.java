package lsfusion.server.mail;

import lsfusion.interop.FormPrintType;

@Deprecated
public enum AttachmentFormat {
    PDF, DOCX, HTML, RTF, XLSX, DBF;

    public FormPrintType getFormPrintType() {
        switch (this) {
            case PDF: return FormPrintType.PDF;
            case DOCX: return FormPrintType.DOCX;
            case HTML: return FormPrintType.HTML;
            case RTF: return FormPrintType.RTF;
            case XLSX: return FormPrintType.XLSX;
        }
        return FormPrintType.HTML;
    }
}
