package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
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

//    protected String getDisplayValue(boolean grid) {
//        return grid ? "grid" : "flex";
//    }
    protected void setDisplayValue(Element element, boolean grid) {
        if(grid)
            GwtClientUtils.addClassName(element, "display-grid");
        else
            GwtClientUtils.addClassName(element, "display-flex");
    }

    protected void setDirectionValue(Element parent, boolean vertical) {
        if(vertical)
            GwtClientUtils.addClassName(parent, "flex-dir-vert");
        else
            GwtClientUtils.addClassName(parent, "flex-dir-horz");
    }

    protected void setGridDirection(Element parent, boolean vertical) {
        if(vertical)
            GwtClientUtils.addClassName(parent, "grid-dir-vert");
        else
            GwtClientUtils.addClassName(parent, "grid-dir-horz");
    }

    //    protected String getAlignmentValue(GFlexAlignment justify, boolean grid) {
//        switch (justify) {
//            case START:
//                return grid ? "start" : "flex-start";
//            case CENTER:
//                return "center";
//            case END:
//                return grid ? "end" : "flex-end";
//            case STRETCH:
//                return "stretch";
//        }
//        throw new IllegalStateException("Unknown alignment");
//    }

    public void setupParentDiv(DivElement parent, boolean vertical, FlexPanel.GridLines gridLines, GFlexAlignment contentAlignment, boolean wrap) {
        boolean grid = gridLines != null;

        setDisplayValue(parent, grid);
        if(grid) {
            setGridContentAlignment(parent, vertical, contentAlignment);

            setGridDirection(parent, vertical);

            setGridLines(parent, !vertical, gridLines.getString());
        } else {
            setFlexContentAlignment(parent, contentAlignment);

            if(wrap)
                setFlexWrap(parent);

            setDirectionValue(parent, vertical);
        }
    }

    public void setFlexWrap(Element parent) {
//        parent.getStyle().setProperty("flexWrap", "wrap");
        GwtClientUtils.addClassName(parent, "flex-wrap");
    }

    public void clearFlexWrap(Element parent) {
        GwtClientUtils.removeClassName(parent, "flex-wrap");
//        parent.getStyle().clearProperty("flexWrap");
    }

    //        parent.getStyle().setProperty(grid && !vertical ? "alignContent" : "justifyContent", getAlignmentValue(alignment, grid));
    public void setFlexContentAlignment(Element parent, GFlexAlignment alignment) {
        switch (alignment) {
            case START:
                GwtClientUtils.addClassName(parent, "flex-content-start");
                return;
            case CENTER:
                GwtClientUtils.addClassName(parent, "flex-content-center");
                return;
            case END:
                GwtClientUtils.addClassName(parent, "flex-content-end");
                return;
            case STRETCH:
                GwtClientUtils.addClassName(parent, "flex-content-stretch");
                return;
        }
        throw new IllegalStateException("Unknown alignment");
    }
    public void setGridContentAlignment(DivElement parent, boolean vertical, GFlexAlignment alignment) {
        switch (alignment) {
            case START:
                if(vertical)
                    GwtClientUtils.addClassName(parent, "grid-content-vert-start");
                else
                    GwtClientUtils.addClassName(parent, "grid-content-horz-start");
                return;
            case CENTER:
                if(vertical)
                    GwtClientUtils.addClassName(parent, "grid-content-vert-center");
                else
                    GwtClientUtils.addClassName(parent, "grid-content-horz-center");
                return;
            case END:
                if(vertical)
                    GwtClientUtils.addClassName(parent, "grid-content-vert-end");
                else
                    GwtClientUtils.addClassName(parent, "grid-content-horz-end");
                return;
            case STRETCH:
                if(vertical)
                    GwtClientUtils.addClassName(parent, "grid-content-vert-stretch");
                else
                    GwtClientUtils.addClassName(parent, "grid-content-horz-stretch");
                return;
        }
        throw new IllegalStateException("Unknown alignment");
    }
//    public void setFlexAlignment(DivElement parent, boolean vertical, boolean grid, GFlexAlignment justify) {
//        parent.getStyle().setProperty(grid && !vertical ? "alignContent" : "justifyContent", getAlignmentValue(justify, grid));
//    }

    //  child.getStyle().setProperty(grid && vertical ? "justifySelf" : "alignSelf", getAlignmentValue(alignment, grid));
    public void setFlexAlignment(GFlexAlignment alignment, Element child) {
        switch (alignment) {
            case START:
                GwtClientUtils.addClassName(child, "flex-start");
                return;
            case CENTER:
                GwtClientUtils.addClassName(child, "flex-center");
                return;
            case END:
                GwtClientUtils.addClassName(child, "flex-end");
                return;
            case STRETCH:
                GwtClientUtils.addClassName(child, "flex-stretch");
                return;
        }
        throw new IllegalStateException("Unknown alignment");
    }
    public void setGridAlignment(GFlexAlignment alignment, Element child, boolean vertical) {
        switch (alignment) {
            case START:
                if(vertical)
                    GwtClientUtils.addClassName(child, "grid-horz-start");
                else
                    GwtClientUtils.addClassName(child, "grid-vert-start");
                return;
            case CENTER:
                if(vertical)
                    GwtClientUtils.addClassName(child, "grid-horz-center");
                else
                    GwtClientUtils.addClassName(child, "grid-vert-center");
                return;
            case END:
                if(vertical)
                    GwtClientUtils.addClassName(child, "grid-horz-end");
                else
                    GwtClientUtils.addClassName(child, "grid-vert-end");
                return;
            case STRETCH:
                if(vertical)
                    GwtClientUtils.addClassName(child, "grid-horz-stretch");
                else
                    GwtClientUtils.addClassName(child, "grid-vert-stretch");
                return;
        }
        throw new IllegalStateException("Unknown alignment");
    }
    public void dropFlexAlignment(GFlexAlignment alignment, Element child) {
        switch (alignment) {
            case START:
                GwtClientUtils.removeClassName(child, "flex-start");
                return;
            case CENTER:
                GwtClientUtils.removeClassName(child, "flex-center");
                return;
            case END:
                GwtClientUtils.removeClassName(child, "flex-end");
                return;
            case STRETCH:
                GwtClientUtils.removeClassName(child, "flex-stretch");
                return;
        }
        throw new IllegalStateException("Unknown alignment");
    }
    public void dropGridAlignment(GFlexAlignment alignment, Element child, boolean vertical) {
        switch (alignment) {
            case START:
                if(vertical)
                    GwtClientUtils.removeClassName(child, "grid-horz-start");
                else
                    GwtClientUtils.removeClassName(child, "grid-vert-start");
                return;
            case CENTER:
                if(vertical)
                    GwtClientUtils.removeClassName(child, "grid-horz-center");
                else
                    GwtClientUtils.removeClassName(child, "grid-vert-center");
                return;
            case END:
                if(vertical)
                    GwtClientUtils.removeClassName(child, "grid-horz-end");
                else
                    GwtClientUtils.removeClassName(child, "grid-vert-end");
                return;
            case STRETCH:
                if(vertical)
                    GwtClientUtils.removeClassName(child, "grid-horz-stretch");
                else
                    GwtClientUtils.removeClassName(child, "grid-vert-stretch");
                return;
        }
        throw new IllegalStateException("Unknown alignment");
    }

//    public void setVisible(DivElement parent, boolean visible, boolean grid) {
//        parent.getStyle().setProperty("display", visible ? getDisplayValue(grid) : "none");
//    }

    public FlexPanel.WidgetLayoutData insertChild(Element parent, Element child, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis, boolean vertical, boolean grid) {
        FlexPanel.WidgetLayoutData layoutData = new FlexPanel.WidgetLayoutData(new FlexPanel.FlexLayoutData(flex, flexBasis, shrink), new FlexPanel.AlignmentLayoutData(alignment));

        updateFlex(layoutData.flex, child, vertical, grid);
        setAlignment(layoutData.aligment, child, vertical, grid);

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
        setFlex(child, 0, layoutData.baseFlexBasis, layoutData.gridLine, vertical, grid, false);
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
        String[] linesArray = GwtSharedUtils.toArray("0px", count);
        parent.setPropertyObject("linesArray", linesArray);

        updateGridLines(parent, linesArray, vertical);
    }

    private static void updateGridLineSize(Element child, int gridLine, boolean vertical, String sizeString) {
        Element parentElement = child.getParentElement();

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

        updateGridLines(parentElement, lines, vertical);
    }

    private static void updateGridLines(Element element, String[] lines, boolean vertical) {
        setGridLines(element, vertical, GwtSharedUtils.toString(" ", lines.length, lines));
    }

    private static void setGridLines(Element element, boolean vertical, String gridLines) {
        element.getStyle().setProperty("gridTemplate" + (vertical ? "Rows" : "Columns"), gridLines);
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
        child.getStyle().setProperty("grid" + (vertical ? "Row" : "Column") + "End", "span " + span);
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

    public int getColumnGap(Element child) {
        return GwtClientUtils.getColumnGap(child);
    }

     public int getMargins(Element child, boolean vertical) {
        return child.getPropertyInt(vertical ? "marginTop" : "marginLeft") +
                    child.getPropertyInt(vertical ? "marginBottom" : "marginRight");
    }

    public void updateFlex(FlexPanel.FlexLayoutData layoutData, Element child, boolean vertical, boolean grid) {
        setFlex(child, layoutData.getFlex(), layoutData.getFlexBasis(), layoutData.gridLine, vertical, grid, layoutData.shrink);
    }
    // unlike flex we have to do it with the set / drop operations, since we are using css classes
    public void setAlignment(FlexPanel.AlignmentLayoutData layoutData, Element child, boolean vertical, boolean grid) {
        GFlexAlignment alignment = layoutData.alignment;
        if(grid)
            setGridAlignment(alignment, child, vertical);
        else
            setFlexAlignment(alignment, child);
    }
    public void dropAlignment(FlexPanel.AlignmentLayoutData layoutData, Element child, boolean vertical, boolean grid) {
        GFlexAlignment alignment = layoutData.alignment;
        if(grid)
            dropGridAlignment(alignment, child, vertical);
        else
            dropFlexAlignment(alignment, child);
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
