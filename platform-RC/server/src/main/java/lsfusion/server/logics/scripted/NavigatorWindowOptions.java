package lsfusion.server.logics.scripted;

import lsfusion.interop.form.layout.Alignment;

public class NavigatorWindowOptions {

    private Boolean drawTitle;
    private Boolean drawRoot;
    private Boolean drawScrollBars;

    private Orientation orientation;
    private BorderPosition borderPosition;

    private DockPosition dockPosition;

    private Alignment vAlign;
    private Alignment hAlign;

    private Alignment textVAlign;
    private Alignment textHAlign;

    public Boolean getDrawTitle() {
        return drawTitle;
    }

    public void setDrawTitle(Boolean drawTitle) {
        this.drawTitle = drawTitle;
    }

    public Boolean getDrawRoot() {
        return drawRoot;
    }

    public void setDrawRoot(Boolean drawRoot) {
        this.drawRoot = drawRoot;
    }

    public Boolean getDrawScrollBars() {
        return drawScrollBars;
    }

    public void setDrawScrollBars(Boolean drawScrollBars) {
        this.drawScrollBars = drawScrollBars;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public BorderPosition getBorderPosition() {
        return borderPosition;
    }

    public void setBorderPosition(BorderPosition borderPosition) {
        this.borderPosition = borderPosition;
    }

    public DockPosition getDockPosition() {
        return dockPosition;
    }

    public void setDockPosition(DockPosition dockPosition) {
        this.dockPosition = dockPosition;
    }

    public Alignment getVAlign() {
        return vAlign;
    }

    public void setVAlign(Alignment vAlign) {
        this.vAlign = vAlign;
    }

    public Alignment getHAlign() {
        return hAlign;
    }

    public void setHAlign(Alignment hAlign) {
        this.hAlign = hAlign;
    }

    public Alignment getTextVAlign() {
        return textVAlign;
    }

    public void setTextVAlign(Alignment textVAlign) {
        this.textVAlign = textVAlign;
    }

    public Alignment getTextHAlign() {
        return textHAlign;
    }

    public void setTextHAlign(Alignment textHAlign) {
        this.textHAlign = textHAlign;
    }

    @Override
    public String toString() {
        return "NavigatorWindowOptions{" +
               "drawTitle=" + drawTitle +
               ", drawRoot=" + drawRoot +
               ", drawScrollBars=" + drawScrollBars +
               ", orientation=" + orientation +
               ", borderPosition=" + borderPosition +
               ", dockPosition=" + dockPosition +
               ", vAlign=" + vAlign +
               ", hAlign=" + hAlign +
               ", textVAlign=" + textVAlign +
               ", textHAlign=" + textHAlign +
               '}';
    }
}
