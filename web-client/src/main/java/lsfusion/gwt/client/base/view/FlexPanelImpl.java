package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;

public class FlexPanelImpl {

    private static FlexPanelImpl impl;

    public static FlexPanelImpl get() {
        if (impl == null) {
            impl = GWT.create(FlexPanelImpl.class);
        }
        return impl;
    }

    protected String getDisplayFlexValue(boolean grid) {
        return grid ? "grid" : "flex";
    }

    protected String getDirectionAttrName() {
        return "flexDirection";
    }

    protected String getDirectionValue(boolean vertical) {
        return vertical ? getVertDirectionValue() : getHorzDirectionValue();
    }

    protected String getHorzDirectionValue() {
        return "row";
    }

    protected String getVertDirectionValue() {
        return "column";
    }

    protected String getJustifyContentAttrName() {
        return "justifyContent";
    }

    protected String getStartAlignmentValue() {
        return "flex-start";
    }

    protected String getCenterAlignmentValue() {
        return "center";
    }

    protected String getEndAlignmentValue() {
        return "flex-end";
    }

    protected String getStretchAlignmentValue() {
        return "stretch";
    }

    protected String getAlignAttrName() {
        return "alignSelf";
    }

    protected String getAlignmentValue(GFlexAlignment justify) {
        switch (justify) {
            case START: return getStartAlignmentValue();
            case CENTER: return getCenterAlignmentValue();
            case END: return getEndAlignmentValue();
            case STRETCH: return getStretchAlignmentValue();
        }
        throw new IllegalStateException("Unknown alignment");
    }

    protected String getFlexAttrName() {
        return "flex";
    }

    public void setupParentDiv(DivElement parent, boolean vertical, Integer gridColumns, FlexPanel.FlexLayoutData grid, GFlexAlignment justify) {
        parent.getStyle().setProperty("display", getDisplayFlexValue(gridColumns != null));
        parent.getStyle().setProperty(getDirectionAttrName(), getDirectionValue(vertical));
        parent.getStyle().setProperty(getJustifyContentAttrName(), getAlignmentValue(justify));
        if(gridColumns != null)
            updateGridLines(parent.getStyle(), GwtSharedUtils.toArray(getLineSizeString(grid.flex, grid.flexBasis), gridColumns), !vertical);
    }

    public void setVisible(DivElement parent, boolean visible, boolean grid) {
        parent.getStyle().setProperty("display", visible ? getDisplayFlexValue(grid) : "none");
    }

    public FlexPanel.WidgetLayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, Integer flexBasis, boolean vertical) {
        FlexPanel.WidgetLayoutData layoutData = new FlexPanel.WidgetLayoutData(new FlexPanel.FlexLayoutData(flex, flexBasis), new FlexPanel.AlignmentLayoutData(alignment));

        updateFlex(layoutData.flex, child, vertical);
        updateAlignment(layoutData.aligment, child);

        DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

    public String getFlexBasisString(Integer flexBasis) {
        return flexBasis == null ? "auto" : flexBasis + "px";
    }

    public void setAutoSizeFlex(Element child, FlexPanel.FlexLayoutData layoutData, boolean vertical) {
        setFlex(child, 0, null, layoutData.gridLine, vertical);
    }

    public void setGridLines(Element parent, int count, boolean vertical) {
        Style style = parent.getStyle();

        String[] linesArray = GwtSharedUtils.toArray("0px", count);
        parent.setPropertyObject("linesArray", linesArray);

        updateGridLines(style, linesArray, vertical);
    }

    private static String getGridLinesAttrName(boolean vertical) {
        return "gridTemplate" + (vertical ? "Rows" : "Columns");
    }
    private static String getGridSpanAttrName(boolean vertical) {
        return "grid" + (vertical ? "Column" : "Row") + "End";
    }

    private static void updateGridLineSize(Element child, int gridLine, boolean vertical, String sizeString) {
        Element parentElement = child.getParentElement();
        Style style = parentElement.getStyle();

        // converting to array
        String[] lines = (String[]) parentElement.getPropertyObject("linesArray");

//        // extending array if needed
//        if(gridLine >= lines.length) {
//            String[] copyLines = new String[gridLine + 1];
//            for(int i = 0 ; i <= gridLine ; i++)
//                copyLines[i] = i >= lines.length ? DROPCOLUMNSTRING : lines[i];
//            lines = copyLines;
//        }

        // setting size
        lines[gridLine] = sizeString;

//        // removing DROPCOLUMN trail
//        int size = lines.length;
//        while(size >= 1 && lines[size - 1].equals(DROPCOLUMNSTRING))
//            size--;

        updateGridLines(style, lines, vertical);
    }

    private static void updateGridLines(Style style, String[] lines, boolean vertical) {
        style.setProperty(getGridLinesAttrName(vertical), GwtSharedUtils.toString(" ", lines.length, lines));
    }

    //    private static String DROPCOLUMNSTRING = "-1px";
    private static String getLineSizeString(double flex, Integer flexBasis) {
        String flexBasisString = flexBasis == null ? "auto" : flexBasis + "px";
        if(flex > 0)
            return "minmax(" + flexBasisString + "," + flex + "fr)";

        return flexBasisString;
    }

    public void setFlex(Element child, double flex, Integer flexBasis, Integer gridLine, boolean vertical) {
        // it's important to set min-width, min-height, because flex-basis is automatically set to min-height if it's smaller (test case in LinearContainerView)
        FlexPanel.setBaseSize(child, vertical, flexBasis, false); // last parameter is false because we're setting main size

        if(gridLine != null)
            updateGridLineSize(child, gridLine, vertical, getLineSizeString(flex, flexBasis));
        else
            child.getStyle().setProperty(getFlexAttrName(), getFlexValue(flex, getFlexBasisString(flexBasis)));
    }

    public void setSpan(Element child, int span, boolean vertical) {
        child.getStyle().setProperty(getGridSpanAttrName(vertical), "span " + span);
    }

    public int getFullSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getFullHeight(child) : GwtClientUtils.getFullWidth(child);
//        return child.getPropertyInt(vertical ? "offsetHeight" : "offsetWidth");
    }

    public int getSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getHeight(child) : GwtClientUtils.getWidth(child);
//        return child.getPropertyInt(vertical ? "clientHeight" : "clientWidth") -
//                (child.getPropertyInt(vertical ? "paddingTop" : "paddingLeft") +
//                        child.getPropertyInt(vertical ? "paddingBottom" : "paddingRight"));
    }

    public int getMargins(Element child, boolean vertical) {
        return child.getPropertyInt(vertical ? "marginTop" : "marginLeft") +
                    child.getPropertyInt(vertical ? "marginBottom" : "marginRight");
    }

    public void updateFlex(FlexPanel.FlexLayoutData layoutData, Element child, boolean vertical) {
        setFlex(child, layoutData.flex, layoutData.flexBasis, layoutData.gridLine, vertical);
    }

    public void updateAlignment(FlexPanel.AlignmentLayoutData layoutData, Element child) {
        child.getStyle().setProperty(getAlignAttrName(), getAlignmentValue(layoutData.alignment));
    }

    public void setFlexBasis(FlexPanel.FlexLayoutData layoutData, Element child, int flexBasis, boolean vertical) {
        layoutData.setFlexBasis(flexBasis); // also sets base flex basis
        updateFlex(layoutData, child, vertical);
    }
    public void setFlex(FlexPanel.FlexLayoutData layoutData, Element child, double flex, Integer flexBasis, boolean vertical) {
        layoutData.flex = flex;
        layoutData.flexBasis = flexBasis;
        updateFlex(layoutData, child, vertical);
    }

    public void setStretchFlex(FlexPanel.FlexLayoutData layoutData, Element child, boolean set, boolean vertical) {
        if(!layoutData.isFlex()) {
            double newFlex = set ? 1 : layoutData.baseFlex;
            if(layoutData.flex != newFlex) { // for optimization purposes + there might be problems with setBaseSize, since some data components use it explicitly without setting LayoutData
                layoutData.flex = newFlex;
                updateFlex(layoutData, child, vertical);
            }
        }
    }

    public void setStretchAlignment(FlexPanel.AlignmentLayoutData layoutData, Element child, boolean set, boolean vertical) {
        if(!layoutData.baseAlignment.equals(GFlexAlignment.STRETCH)) {
            GFlexAlignment newAlignment = set ? GFlexAlignment.STRETCH : layoutData.baseAlignment;
            if(!newAlignment.equals(layoutData.alignment)) {
                layoutData.alignment = newAlignment;
                updateAlignment(layoutData, child);
            }
        }
    }

    public void setGridLine(FlexPanel.FlexLayoutData layoutData, Element child, Integer gridLine, boolean vertical) {
        layoutData.gridLine = gridLine;
        updateFlex(layoutData, child, vertical);
    }

    public void setGridSpan(FlexPanel.WidgetLayoutData layoutData, Element child, int span, boolean vertical) {
        layoutData.span = span;
        setSpan(child, span, vertical);
    }

    public void fixFlexBasis(Widget widget, boolean vertical) {
        FlexPanel.FlexLayoutData layoutData = ((FlexPanel.WidgetLayoutData) widget.getLayoutData()).flex;
        if(layoutData.flexBasis != null)
            return;

        Element element = widget.getElement();
        // фиксируем явную ширину composite'а (ставим базис равный ширине, flex - 0)

        if(!layoutData.isFlex()) // оптимизация если flex'а нет, этот размер и фиксируем
            layoutData.setFlexBasis(getSize(element, vertical));
        else {
            setAutoSizeFlex(element, layoutData, vertical);
//            setAlignment(childWidgetElement, GFlexAlignment.START);

            // измеряем ширину, запоминаем в базис
            int calcSize = getSize(element, vertical);

            // выставляем расчитанный базис (возвращаем flex)
            setFlexBasis(layoutData, element, calcSize, vertical);
        }
    }

    private String getFlexValue(double flex, String flexBasis) {
        return flex + " 0 " + flexBasis;
    }
}
