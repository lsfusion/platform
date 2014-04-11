package lsfusion.server.form.view.report;

// поле для отрисовки отчета
public class ReportDrawField implements AbstractRowLayoutElement {

    public String sID;
    public String caption;
    public Class valueClass;
    public Class captionClass;
    public Class footerClass;

    public int minimumWidth;
    private int preferredWidth;
    public Integer fixedCharWidth;
    public byte alignment;

    public int scale;

    public String pattern;

    public boolean hasColumnGroupObjects;
    public boolean hasCaptionProperty;
    public boolean hasFooterProperty;

    private int charWidth;

    public ReportDrawField(String sID, String caption, int charWidth) {
        this.sID = sID;
        this.caption = caption;
        this.charWidth = charWidth;
    }

    public int getCaptionWidth() {
        return caption.length() * charWidth;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public Integer widthUser;
    public void setWidthUser(Integer widthUser){
        this.widthUser = widthUser;
    }

    public int getPreferredWidth() {
        int width;
        if (widthUser != null)
            width = widthUser;
        else if (fixedCharWidth != null) {
            width = fixedCharWidth * charWidth;
        } else {
            width = preferredWidth;
        }
        return Math.max(getMinimumWidth(), Math.min(200 * scale, width));
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
