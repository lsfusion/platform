package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.interop.form.print.FormPrintType;

@Deprecated
public enum AttachmentFormat {
    PDF, DOCX, HTML, RTF, XLSX, DBF, XML;

    @Deprecated
    public String getExtension() {
        switch (this) {
            case PDF: return "pdf";
            case DOCX: return "docx";
            case HTML: return "html";
            case RTF: return "rtf";
            case XLSX: return "xlsx";
            case DBF: return "dbf";
            case XML: return "xml";
        }
        return null;
    }
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
