package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
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

    protected String getDirectionValue(boolean vertical) {
        return vertical ? getVertDirectionValue() : getHorzDirectionValue();
    }

    protected String getHorzDirectionValue() {
        return "row";
    }

    protected String getVertDirectionValue() {
        return "column";
    }

    protected String getAlignmentValue(GFlexAlignment justify, boolean grid) {
        switch (justify) {
            case START:
                return grid ? "start" : "flex-start";
            case CENTER:
                return "center";
            case END:
                return grid ? "end" : "flex-end";
            case STRETCH:
                return "stretch";
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
                setWrap(parent);

            parent.getStyle().setProperty("flexDirection", getDirectionValue(vertical));
        }
    }

    public void setWrap(Element parent) {
        parent.getStyle().setProperty("flexWrap", "wrap");
    }

    public void dropWrap(Element parent) {
        parent.getStyle().clearProperty("flexWrap");
    }

    public void setFlexAlignment(DivElement parent, boolean vertical, boolean grid, GFlexAlignment justify) {
        parent.getStyle().setProperty(!grid || vertical ? "justifyContent" : "alignContent", getAlignmentValue(justify, grid));
    }

    public void setVisible(DivElement parent, boolean visible, boolean grid) {
        parent.getStyle().setProperty("display", visible ? getDisplayValue(grid) : "none");
    }

    public FlexPanel.WidgetLayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis, boolean vertical, boolean grid) {
        FlexPanel.WidgetLayoutData layoutData = new FlexPanel.WidgetLayoutData(new FlexPanel.FlexLayoutData(flex, flexBasis, shrink), new FlexPanel.AlignmentLayoutData(alignment));

        updateFlex(layoutData.flex, child, vertical, grid);
        updateAlignment(layoutData.aligment, child, vertical, grid);

        DOM.insertChild(parent.<com.google.gwt.user.client.Element>cast(), child.<com.google.gwt.user.client.Element>cast(), beforeIndex);

        return layoutData;
    }

    public String getFlexString(double flex) {
        return flex + "";
    }
    public String getFlexBasisString(GSize flexBasis) {
        return flexBasis == null ? "auto" : flexBasis.getString();
    }

    public void setNoFlex(Element child, FlexPanel.FlexLayoutData layoutData, boolean vertical, boolean grid) {
        setFlex(child, 0, layoutData.flexBasis, layoutData.gridLine, vertical, grid, false);
    }

    public void setPreferredSize(boolean set, Element child, FlexPanel.FlexLayoutData layoutData, boolean vertical, boolean grid) {
//        setFlex(child, layoutData.flex, set ? null : layoutData.getFlexBasis(), layoutData.gridLine, vertical, grid, set ? false : layoutData.shrink);
        if(set) { // we're moving flexBasis to min-size
            setFlexParams(child, layoutData.getFlex(), null, layoutData.gridLine, vertical, grid, false);

            FlexPanel.setMinPanelSize(child, vertical, layoutData.getFlexBasis());

            FlexPanel.setPanelSize(child, vertical, null);
        } else
            updateFlex(layoutData, child, vertical, grid);
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
    public static String getLineSizeString(double flex, GSize flexBasis, boolean shrink) {
        // it seems that  min-content is equivalent to auto in flex (and auto in grid layout for example often does not respect margins somewhy)
        String flexBasisString = flexBasis == null ? "min-content" : flexBasis.getString();
        if(shrink)
            return "minmax(0px," + (flex > 0 ? flex + "fr" : flexBasisString) + ")";
        if(flex > 0)
            return "minmax(" + flexBasisString + "," + flex + "fr)";
        return flexBasisString;
    }

    public void setFlex(Element child, double flex, GSize flexBasis, Integer gridLine, boolean vertical, boolean grid, boolean shrink) {
        // it seems that for regular flex we can set flexBasis, but for a grid there is a problem, that grid does not respect margins (see the test case below), so we always set auto (because width / height is set in this method)
        setFlexParams(child, flex, null, gridLine, vertical, grid, shrink); // flexBasis

        // otherwise min-width won't let the container to shrink
        FlexPanel.setMinPanelSize(child, vertical, shrink ? GSize.ZERO : null);

        FlexPanel.setPanelSize(child, vertical, flexBasis);
    }

    private void setFlexParams(Element child, double flex, GSize flexBasis, Integer gridLine, boolean vertical, boolean grid, boolean shrink) {
        if(grid) {
            if(gridLine != null)
                updateGridLineSize(child, gridLine, vertical, getLineSizeString(flex, flexBasis, shrink));
        } else
            child.getStyle().setProperty("flex", getFlexString(flex) + " " + getFlexString(shrink ? 1 : 0) + " " + getFlexBasisString(flexBasis));
    }

    //    <div style="display:flex;flex-direction:column;height:300px">
//  <div style="flex:0 0 auto;margin:30px;background-color:blue;height:100px">
//    AAAA
//            </div>
//  <div style="flex:1 0 0px;background-color:red">
//    BBBB
//            </div>
//</div>
//
//<div style="display:grid;grid-template-rows:100px minmax(min-content, 1fr);grid-template-columns:1fr;height:300px">
//  <div style="flex:0 0 100px;margin:30px;background-color:blue;height:100px">
//    AAAA
//            </div>
//  <div style="flex:1 0 0px;background-color:red">
//    BBBB
//            </div>
//</div>


    public void setSpan(Element child, int span, boolean vertical) {
        child.getStyle().setProperty(getGridSpanAttrName(vertical), "span " + span);
    }

    public int getFullSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getFullHeight(child) : GwtClientUtils.getFullWidth(child);
//        return child.getPropertyInt(vertical ? "offsetHeight" : "offsetWidth");
    }

    public GSize getOffsetSize(Element child, boolean vertical) {
        return vertical ? GwtClientUtils.getOffsetHeight(child) : GwtClientUtils.getOffsetWidth(child);
    }
    public int getActualSize(Element child, boolean vertical) {
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
        child.getStyle().setProperty(grid && vertical ? "justifySelf" : "alignSelf", getAlignmentValue(layoutData.alignment, grid));
    }

    public void setGridAlignment(Element child, boolean vertical, GFlexAlignment alignment) {
        child.getStyle().setProperty(vertical ? "alignSelf" : "justifySelf", getAlignmentValue(alignment, true));
    }

    public void setFlexBasis(FlexPanel.FlexLayoutData layoutData, Element child, GSize flexBasis, boolean vertical, boolean grid) {
        layoutData.setFlexBasis(flexBasis); // also sets base flex basis
        updateFlex(layoutData, child, vertical, grid);
    }
    public void setFlex(FlexPanel.FlexLayoutData layoutData, Element child, double flex, GSize flexBasis, boolean vertical, boolean grid) {
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
            layoutData.setFlexBasis(getOffsetSize(element, vertical));
        else {
            setNoFlex(element, layoutData, vertical, grid);
//            setAlignment(childWidgetElement, GFlexAlignment.START);

            // измеряем ширину, запоминаем в базис
            GSize calcSize = getOffsetSize(element, vertical);

            // выставляем расчитанный базис (возвращаем flex)
            setFlexBasis(layoutData, element, calcSize, vertical, grid);
        }
    }

    private String getFlexValue(double flex, String flexBasis) {
        return flex + " 0 " + flexBasis;
    }
}
