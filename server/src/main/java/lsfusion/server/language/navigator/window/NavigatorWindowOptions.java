package lsfusion.server.language.navigator.window;

import lsfusion.interop.base.view.FlexAlignment;

public class NavigatorWindowOptions {

    private Boolean drawTitle;
    private Boolean drawRoot;
    private Boolean drawScrollBars;

    private Orientation orientation;
    private BorderPosition borderPosition;

    private DockPosition dockPosition;

    private FlexAlignment vAlign;
    private FlexAlignment hAlign;

    private FlexAlignment textVAlign;
    private FlexAlignment textHAlign;

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

    public FlexAlignment getVAlign() {
        return vAlign;
    }

    public void setVAlign(FlexAlignment vAlign) {
        this.vAlign = vAlign;
    }

    public FlexAlignment getHAlign() {
        return hAlign;
    }

    public void setHAlign(FlexAlignment hAlign) {
        this.hAlign = hAlign;
    }

    public FlexAlignment getTextVAlign() {
        return textVAlign;
    }

    public void setTextVAlign(FlexAlignment textVAlign) {
        this.textVAlign = textVAlign;
    }

    public FlexAlignment getTextHAlign() {
        return textHAlign;
    }

    public void setTextHAlign(FlexAlignment textHAlign) {
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
