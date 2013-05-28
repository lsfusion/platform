package platform.server.mail;

public enum AttachmentFormat {
    PDF, DOCX, HTML, RTF;

    public String getExtension() {
        switch (this) {
            case PDF: return ".pdf";
            case DOCX: return ".docx";
            case HTML: return ".html";
            case RTF: return ".rtf";
        }
        return null;
    }
}
