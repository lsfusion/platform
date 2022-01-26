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

    protected String getDisplayValue(boolean grid) {
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

    protected String getStartAlignmentValue(boolean grid) {
        return grid ? "start" : "flex-start";
    }

    protected String getCenterAlignmentValue() {
        return "center";
    }

    protected String getEndAlignmentValue(boolean grid) {
        return grid ? "end" : "flex-end";
    }

    protected String getStretchAlignmentValue() {
        return "stretch";
    }

    protected String getJustifyContentAttrName(boolean grid, boolean vertical) {
        if(!grid || vertical)
            return "justifyContent";
        else
            return "alignContent";
    }

    protected String getAlignAttrName(boolean grid, boolean vertical) {
        if(!grid || !vertical)
            return "alignSelf";
        else
            return "justifySelf";
    }

    protected String getAlignmentValue(GFlexAlignment justify, boolean grid) {
        switch (justify) {
            case START: return getStartAlignmentValue(grid);
            case CENTER: return getCenterAlignmentValue();
            case END: return getEndAlignmentValue(grid);
            case STRETCH: return getStretchAlignmentValue();
        }
        throw new IllegalStateException("Unknown alignment");
    }

    public void setupParentDiv(DivElement parent, boolean vertical, FlexPanel.GridLines gridLines, GFlexAlignment justify, boolean wrap) {
        boolean grid = gridLines != null;

        parent.getStyle().setProperty("display", getDisplayValue(grid));
        setFlexAlignment(parent, vertical, grid, justify);
        if(grid) {
            parent.getStyle().setProperty("gridAutoFlow", vertical ? "row" : "column");

            parent.getStyle().setProperty(getGridLinesAttrName(!vertical), gridLines.getString());
        } else {
            if(wrap)
                parent.getStyle().setProperty("flexWrap", "wrap");

            parent.getStyle().setProperty(getDirectionAttrName(), getDirectionValue(vertical));
        }
    }

    public void setFlexAlignment(DivElement parent, boolean vertical, boolean grid, GFlexAlignment justify) {
        parent.getStyle().setProperty(getJustifyContentAttrName(grid, vertical), getAlignmentValue(justify, grid));
    }

    public void setVisible(DivElement parent, boolean visible, boolean grid) {
        parent.getStyle().setProperty("display", visible ? getDisplayValue(grid) : "none");
    }

    public FlexPanel.WidgetLayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, Integer flexBasis, boolean vertical, boolean grid) {
        FlexPanel.WidgetLayoutData layoutData = new FlexPanel.WidgetLayoutData(new FlexPanel.FlexLayoutData(flex, flexBasis, shrink), new FlexPanel.AlignmentLayoutData(alignment));

        updateFlex(layoutData.flex, child, vertical, grid);
        updateAlignment(layoutData.aligment, child, vertical, grid);

        DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

    public String getFlexString(double flex) {
        return flex + "";
    }
    public String getFlexBasisString(Integer flexBasis) {
        return flexBasis == null ? "auto" : flexBasis + "px";
    }

    public void setAutoSizeFlex(Element child, FlexPanel.FlexLayoutData layoutData, boolean vertical, boolean grid) {
        setFlex(child, 0, null, layoutData.gridLine, vertical, grid, false);
    }

    public void setPreferredSize(boolean set, Element child, FlexPanel.FlexLayoutData layoutData, boolean vertical, boolean grid) {
        setFlex(child, layoutData.flex, set ? null : layoutData.getFlexBasis(), layoutData.gridLine, vertical, grid, set ? false : layoutData.shrink);
//        setFlexParams(child, layoutData.getFlex(), set ? null : layoutData.flexBasis, layoutData.gridLine, vertical, grid, layoutData.shrink);
//
//        if(layoutData.shrink) // if we have shrink we want to drop it and have actual min-size
//            FlexPanel.setBaseSize(child, vertical, layoutData.flexBasis, true); // last parameter is false because we're setting main size
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
        return "grid" + (vertical ? "Row" : "Column") + "End";
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
    public static String getLineSizeString(double flex, Integer flexBasis, boolean shrink) {
        // it seems that  min-content is equivalent to auto in flex (and auto in grid layout for example often does not respect margins somewhy)
        String flexBasisString = flexBasis == null ? (shrink ? "0px" : "min-content") : flexBasis + "px";
        if(flex > 0)
            return "minmax(" + flexBasisString + "," + flex + "fr)";

        return flexBasisString;
    }

    public void setFlex(Element child, double flex, Integer flexBasis, Integer gridLine, boolean vertical, boolean grid, boolean shrink) {
        setFlexParams(child, flex, flexBasis, gridLine, vertical, grid, shrink);

        // otherwise min-width won't let the container to shrink
        FlexPanel.setBaseSize(child, vertical, shrink ? 0 : null, false);

        // it's important to set min-width, min-height, because flex-basis is automatically set to min-height if it's smaller (test case in LinearContainerView)
        FlexPanel.setBaseSize(child, vertical, flexBasis, true); // last parameter is false because we're setting main size
    }

    private void setFlexParams(Element child, double flex, Integer flexBasis, Integer gridLine, boolean vertical, boolean grid, boolean shrink) {
        if(grid) {
            if(gridLine != null)
                updateGridLineSize(child, gridLine, vertical, getLineSizeString(flex, flexBasis, shrink));
        } else
            child.getStyle().setProperty("flex", getFlexString(flex) + " " + getFlexString(shrink ? 1 : 0) + " " + getFlexBasisString(flexBasis));
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

    public void updateFlex(FlexPanel.FlexLayoutData layoutData, Element child, boolean vertical, boolean grid) {
        setFlex(child, layoutData.getFlex(), layoutData.getFlexBasis(), layoutData.gridLine, vertical, grid, layoutData.shrink);
    }

    public void updateAlignment(FlexPanel.AlignmentLayoutData layoutData, Element child, boolean vertical, boolean grid) {
        child.getStyle().setProperty(getAlignAttrName(grid, vertical), getAlignmentValue(layoutData.alignment, grid));
    }

    public void setFlexBasis(FlexPanel.FlexLayoutData layoutData, Element child, int flexBasis, boolean vertical, boolean grid) {
        layoutData.setFlexBasis(flexBasis); // also sets base flex basis
        updateFlex(layoutData, child, vertical, grid);
    }
    public void setFlex(FlexPanel.FlexLayoutData layoutData, Element child, double flex, Integer flexBasis, boolean vertical, boolean grid) {
        layoutData.flex = flex;
        layoutData.flexBasis = flexBasis;
        updateFlex(layoutData, child, vertical, grid);
    }

    public void setGridLine(FlexPanel.FlexLayoutData layoutData, Element child, Integer gridLine, boolean vertical) {
        layoutData.gridLine = gridLine;
        updateFlex(layoutData, child, vertical, true);
    }

    public void setGridSpan(FlexPanel.WidgetLayoutData layoutData, Element child, int span, boolean vertical) {
        layoutData.span = span;
        setSpan(child, span, vertical);
    }

    public void fixFlexBasis(Widget widget, boolean vertical, boolean grid) {
        FlexPanel.FlexLayoutData layoutData = ((FlexPanel.WidgetLayoutData) widget.getLayoutData()).flex;
        if(layoutData.getFlexBasis() != null)
            return;

        Element element = widget.getElement();
        // фиксируем явную ширину composite'а (ставим базис равный ширине, flex - 0)

        if(!layoutData.isFlex()) // оптимизация если flex'а нет, этот размер и фиксируем
            layoutData.setFlexBasis(getSize(element, vertical));
        else {
            setAutoSizeFlex(element, layoutData, vertical, grid);
//            setAlignment(childWidgetElement, GFlexAlignment.START);

            // измеряем ширину, запоминаем в базис
            int calcSize = getSize(element, vertical);

            // выставляем расчитанный базис (возвращаем flex)
            setFlexBasis(layoutData, element, calcSize, vertical, grid);
        }
    }

    private String getFlexValue(double flex, String flexBasis) {
        return flex + " 0 " + flexBasis;
    }
}
