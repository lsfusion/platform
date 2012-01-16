package platform.server.logics.scripted;

public class NavigatorWindowOptions {

    private Boolean drawTitle;
    private Boolean drawRoot;
    private Boolean drawScrollBars;

    private Orientation orientation;
    private BorderPosition borderPosition;

    private DockPosition dockPosition;

    private VAlign vAlign;
    private HAlign hAlign;

    private VAlign textVAlign;
    private HAlign textHAlign;

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

    public VAlign getVAlign() {
        return vAlign;
    }

    public void setVAlign(VAlign vAlign) {
        this.vAlign = vAlign;
    }

    public HAlign getHAlign() {
        return hAlign;
    }

    public void setHAlign(HAlign hAlign) {
        this.hAlign = hAlign;
    }

    public VAlign getTextVAlign() {
        return textVAlign;
    }

    public void setTextVAlign(VAlign textVAlign) {
        this.textVAlign = textVAlign;
    }

    public HAlign getTextHAlign() {
        return textHAlign;
    }

    public void setTextHAlign(HAlign textHAlign) {
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
