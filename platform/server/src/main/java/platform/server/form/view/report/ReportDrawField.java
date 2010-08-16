package platform.server.form.view.report;

// поле для отрисовки отчета
public class ReportDrawField implements AbstractRowLayoutElement {

    public String sID;
    public String caption;
    public Class valueClass;

    public int minimumWidth;
    public int preferredWidth;
    public byte alignment;

    public String pattern;

    public ReportDrawField(String sID, String caption) {
        this.sID = sID;
        this.caption = caption;
    }

    public int getCaptionWidth() {
        return caption.length() * 10;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public int left;
    public void setLeft(int ileft) {
        left = ileft;
    }

    public int width;
    public void setWidth(int iwidth) {
        width = iwidth;
    }

    public int row;
    public void setRow(int irow) {
        row = irow;
    }
}
