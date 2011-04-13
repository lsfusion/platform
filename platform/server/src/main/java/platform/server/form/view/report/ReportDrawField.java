package platform.server.form.view.report;

// поле для отрисовки отчета
public class ReportDrawField implements AbstractRowLayoutElement {

    public String sID;
    public String caption;
    public Class valueClass;
    public Class captionClass;

    public int minimumWidth;
    private int preferredWidth;
    public Integer fixedCharWidth;
    public byte alignment;

    public String pattern;

    public boolean hasColumnGroupObjects;
    public boolean hasCaptionProperty;

    static final int charWidth = 8;

    public ReportDrawField(String sID, String caption) {
        this.sID = sID;
        this.caption = caption;
    }

    public int getCaptionWidth() {
        return caption.length() * charWidth;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public int getPreferredWidth() {
        int width;
        if (fixedCharWidth != null) {
            width = fixedCharWidth * charWidth;
        } else {
            width = preferredWidth;
        }
        return Math.max(getMinimumWidth(), Math.min(200, width));
    }

    public void setPreferredWidth(int width) {
        preferredWidth = width;
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
