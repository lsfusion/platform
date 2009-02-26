package platform.server.view.form.client.report;

import platform.server.view.form.client.CellView;

// поле для отрисовки отчета
public class ReportDrawField implements AbstractRowLayoutElement {

    public String sID;
    public String caption;
    public Class valueClass;

    public int minimumWidth;
    public int preferredWidth;
    public byte alignment;

    public String pattern;

    public ReportDrawField(CellView cellView) {
        cellView.fillReportDrawField(this);
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
