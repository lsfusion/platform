package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.resize.ResizeHelper;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.table.TableContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FlexPanel extends ComplexPanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize {

    protected static FlexPanelImpl impl = FlexPanelImpl.get();

    private final DivElement parentElement;

    private final boolean vertical;

    private GFlexAlignment flexAlignment;

    public final boolean wrap;

    private boolean visible = true;

    public boolean childrenResizable = true;

    public FlexPanel() {
        this(false);
    }

    public FlexPanel(boolean vertical) {
        this(vertical, GFlexAlignment.START);
    }

    private final GridLines gridLines;
    
    public FlexPanel(boolean vertical, GFlexAlignment flexAlignment) {
        this(vertical, flexAlignment, null, false);
    }

    public FlexPanel(boolean vertical, GFlexAlignment flexAlignment, GridLines gridLines, boolean wrap) {
        this.vertical = vertical;

        this.gridLines = gridLines;

        this.wrap = wrap;

        this.flexAlignment = flexAlignment;

        parentElement = Document.get().createDivElement();

        impl.setupParentDiv(parentElement, vertical, gridLines, flexAlignment, wrap);

        setElement(parentElement);

        DataGrid.initSinkMouseEvents(this);
    }

    public boolean isVertical() {
        return vertical;
    }

    public boolean isHorizontal() {
        return !isVertical();
    }

    @Override
    public void setVisible(boolean nVisible) {
        if (visible != nVisible) {
            visible = nVisible;
            super.setVisible(nVisible);
//            impl.setVisible(parentElement, visible, isGrid());
        }
    }

    @Override
    public void add(Widget child) {
        add(child, GFlexAlignment.START);
    }
    
    public void addCentered(Widget child) {
        add(child, GFlexAlignment.CENTER);
    }

    public void addStretched(Widget child) {
        add(child, GFlexAlignment.STRETCH);
    }

    public void add(Widget widget, GFlexAlignment alignment) {
        add(widget, getWidgetCount(), alignment);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment) {
        add(widget, beforeIndex, alignment, 0, false, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addFill(Widget widget) {
        addFill(widget, getWidgetCount());
    }

    public void addFillFlex(Widget widget, GSize flexBasis) {
        addFill(widget, getWidgetCount(), flexBasis);
    }

    public void addShrinkFlex(Widget widget, GSize flexBasis) {
        add(widget, GFlexAlignment.STRETCH, 0, true, flexBasis);
    }

    public void addFill(Widget widget, int beforeIndex, GSize flexBasis) {
        add(widget, beforeIndex, GFlexAlignment.STRETCH, 1, false, flexBasis);
    }

    public void addFillShrink(Widget widget) {
        add(widget, GFlexAlignment.STRETCH, 1, true, null);
    }

    public void add(Widget widget, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis) {
        add(widget, getWidgetCount(), alignment, flex, shrink, flexBasis);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis) {
        // Detach new child.
        widget.removeFromParent();

        // Logical attach.
        getChildren().insert(widget, beforeIndex);

        // Physical attach.
        Element childElement = widget.getElement();

        WidgetLayoutData layoutData = impl.insertChild(parentElement, childElement, beforeIndex, alignment, flex, shrink, flexBasis, vertical, isGrid());
        widget.setLayoutData(layoutData);

        // Adopt.
        adopt(widget);
    }

    // it's tricky here
    // it seems that flex basis auto works different in vertical (column) and horizontal (row) directions
    // in horizontal direction it seems that flex basis does not respect descendants flex-basis, and in vertical direction it does
    // test case in jsfiddle (changing direction to column doesn't work)
    // <div style="overflow: hidden;display: flex;flex-direction: row; position:absolute; top:0; left:0; right:0; bottom:0;">
    //  <div style="overflow: hidden;display: flex;flex-direction: row;flex: 0 0 auto;">
    //     <div style="overflow: hidden;display: flex;flex-direction: row;flex: 1 0 0px;">
    //       <div>TEXT</div>
    //     </div>
    //  </div>
    //</div>
    // so for vertical direction we keep auto since it works predictable and as expected
    // for horizontal direction we set 0 - basis since it doesn't influence on other autos
    // for now all that is important only for autoSize props, but in theory can be important in other cases
    public void addFill(Widget widget, int beforeIndex) {
        addFill(widget, beforeIndex, null);
    }

    // we're setting min-width/height and not width/height for two reasons:
    // a) alignment STRETCH doesn't work when width is set (however for the alignment other than STRETCH min param works as max of this min size, and auto size, and this behaviour is different from flex:0 0 size what we want to get)
    // b) !!!(not relevant, because width / height could/should be used) flexBasis auto doesn't respect flexBasis of its descendants (!!! it's not true for vertical direction, see addFill comment !!!), but respects min-width (however with that approach in future there might be some problems with flex-shrink if we we'll want to support it)

    // everything panel sizing related
    public static void setMinPanelSize(Element element, boolean vertical, GSize size) {
        if(vertical)
            setMinPanelHeight(element, size);
        else
            setMinPanelWidth(element, size);
    }
    public static void setPanelSize(Element element, boolean vertical, GSize size) {
        if(vertical)
            setHeight(element, size);
        else
            setPanelWidth(element, size);
    }
    public static void setPanelWidth(Element element, GSize size) {
        setWidth(element, size);
    }
    public static void setMinPanelWidth(Element element, GSize size) {
        setMinPanelWidth(element, size != null ? size.getString() : null);
    }
    public static void setMinPanelHeight(Element element, GSize size) {
        setSizeProperty(element, "minHeight", size != null ? size.getString() : null);
    }
    public static void setMinPanelWidth(Element element, String size) {
        setSizeProperty(element, "minWidth", size);
    }

    // everything grid sizing related
    public static void setGridWidth(Element element, String size) {
        setWidth(element, size);
    }
    public static void setGridHeight(Element element, GSize size) {
        setHeight(element, size);
    }

    // everything pref sizing (auto dialog size) related
    public static void setPrefWidth(Element element, GSize size) {
        setWidth(element, size);
    }
    public static void setPrefHeight(Element element, GSize size) {
        setHeight(element, size);
    }
    public static void setMaxPrefWidth(Element element, GSize size) {
        setMaxWidth(element, size);
    }
    public static void setMaxPrefHeight(Element element, GSize size) {
        setMaxHeight(element, size);
    }
    public static void setMaxPrefWidth(Element element, String size) {
        setMaxWidth(element, size);
    }

    private static void setWidth(Element element, GSize size) {
        setWidth(element, size != null ? size.getString() : null);
    }
    private static void setWidth(Element element, String size) {
        setSizeProperty(element, "width", size);
    }
    private static void setHeight(Element element, GSize size) {
        setSizeProperty(element, "height", size != null ? size.getString() : null);
    }
    private static void setMaxWidth(Element element, GSize size) {
        setMaxWidth(element, size != null ? size.getString() : null);
    }
    private static void setMaxHeight(Element element, GSize size) {
        setSizeProperty(element, "maxHeight", size != null ? size.getString() : null);
    }
    private static void setMaxWidth(Element element, String size) {
        setSizeProperty(element, "maxWidth", size);
    }
    private static void setSizeProperty(Element element, String propName, String size) {
        Style style = element.getStyle();
        if(size != null)
            style.setProperty(propName, size);
        else
            style.clearProperty(propName);
    }

    public static void setSpan(Widget w, int span, boolean vertical) {
        impl.setGridSpan(((WidgetLayoutData) w.getLayoutData()), w.getElement(), span, vertical);
    }

    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        for (Widget child : getChildren()) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        checkResizeEvent(event, getElement());
    }

    public void checkResizeEvent(NativeEvent event, Element cursorElement) {
        ResizeHandler.checkResizeEvent(getResizeHelper(event), cursorElement, null, event);
    }

    public interface GridLines {

        FlexLayoutData getLineLayoutData(int i);
        int getCount();

        String getString();
    }

    public static class GridWrapLines implements GridLines {

        public final FlexLayoutData lineSize;

        public GridWrapLines(FlexLayoutData lineSize) {
            this.lineSize = lineSize;
        }

        @Override
        public String getString() {
            return "repeat(auto-fit," + FlexPanelImpl.getLineSizeString(lineSize.getFlex(), lineSize.getFlexBasis(), lineSize.shrink) + ")";
        }

        @Override
        public FlexLayoutData getLineLayoutData(int i) {
            return lineSize;
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

    public static class GridFixedLines implements GridLines {

        public final FlexLayoutData[] lines;

        public GridFixedLines(FlexLayoutData[] lines) {
            this.lines = lines;
        }

        @Override
        public FlexLayoutData getLineLayoutData(int i) {
            return lines[i];
        }

        @Override
        public int getCount() {
            return lines.length;
        }

        public String getString() {
            String[] gridColumnStrings = new String[lines.length];
            for(int i = 0; i < lines.length; i++) {
                FlexPanel.FlexLayoutData gridColumn = lines[i];
                gridColumnStrings[i] = FlexPanelImpl.getLineSizeString(gridColumn.getFlex(), gridColumn.getFlexBasis(), gridColumn.shrink);
            }
            return GwtSharedUtils.toString(" ", gridColumnStrings.length, gridColumnStrings);
        }
    }

    enum FlexModifier {
        STRETCH, COLLAPSE
    }

    public static final class FlexLayoutData {

        // now, both are changed in resizeWidget
        public double flex;
        public GSize flexBasis; // changed on autosize and on tab change (in this case baseFlexBasis should be change to)

        public double baseFlex;
        public GSize baseFlexBasis; // if null, than we have to get it similar to fixFlexBases by setting flexes to 0

        public Integer gridLine; // only for "main" widgets in column should be set

        public boolean shrink;

        public FlexModifier flexModifier;

        private Double getModifiedFlex() {
            FlexModifier flexModifier = this.flexModifier;
            if(flexModifier != null) {
                if(flexModifier == FlexModifier.STRETCH)
                    return 1.0;
                if(flexModifier == FlexModifier.COLLAPSE)
                    return 0.0;
            }
            return null;
        }

        public double getBaseFlex() {
            Double modifiedFlex = getModifiedFlex();
            if (modifiedFlex != null)
                return modifiedFlex;

            return baseFlex;
        }

        public double getFlex() {
            Double modifiedFlex = getModifiedFlex();
            if (modifiedFlex != null)
                return modifiedFlex;

            return flex;
        }

        public boolean isFlex() {
            // we can use both baseFlex and flex
            boolean result = getBaseFlex() > 0;
            // falls on resize
//            assert result == getFlex() > 0;
            return result;
        }

        public boolean isAutoSized() {
            return baseFlexBasis == null;
        }

        public void setFlexBasis(GSize flexBasis) {
            this.flexBasis = flexBasis;
            baseFlexBasis = flexBasis;
        }
        
        public GSize getFlexBasis() {
            if (flexModifier == FlexModifier.COLLAPSE) { // in theory it should not just drop to auto, but also set min-height to flexBasis (however it's heuristics anyway, so for now the current behaviour will also do)
                return null;
            }
            return flexBasis;
        }

        public FlexLayoutData(double flex, GSize flexBasis, boolean shrink) {
            this.flex = flex;
            this.baseFlex = flex;

            this.flexBasis = flexBasis;
            baseFlexBasis = flexBasis;

            this.shrink = shrink;
        }
    }

    public static final class AlignmentLayoutData {

        public GFlexAlignment alignment; // changed in setStretchAlignment

        public final GFlexAlignment baseAlignment;

        public AlignmentLayoutData(GFlexAlignment alignment) {
            this.alignment = alignment;

            this.baseAlignment = alignment;
        }

    }

    public static final class WidgetLayoutData {

        public final FlexLayoutData flex;
        public final AlignmentLayoutData aligment;

        public int span = 1;

        public boolean caption = false;

        public WidgetLayoutData(FlexLayoutData flex, AlignmentLayoutData aligment) {
            this.flex = flex;
            this.aligment = aligment;
        }
    }

    public class FlexLine implements FlexStretchLine {

        private final List<Widget> widgets;
        private final Widget widget; // main widget

        public FlexLine(List<Widget> widgets) {
            this.widgets = widgets;

            widget = widgets.get(widgets.size() - 1); // last one
        }

        public void setGridLines(int gridLine) {
            for(Widget widget : widgets)
                impl.setGridLine(((WidgetLayoutData)widget.getLayoutData()).flex, widget.getElement(), widget.equals(this.widget) ? gridLine : null, vertical);
        }

        // resize prepare widget methods

        public boolean isFlex() {
            return getFlexLayoutData().isFlex();
        }

        public boolean contains(Widget widget) {
            return widgets.contains(widget);
        }

        public void propagateChildResizeEvent(NativeEvent event, Element cursorElement) {
            int resizedChild = ResizeHandler.getResizedChild(!vertical, widgets, event);
            if(resizedChild >= 0) {
                Widget resizeWidget = widgets.get(resizedChild);
                if (resizeWidget instanceof FlexPanel)
                    ((FlexPanel)resizeWidget).checkResizeEvent(event, cursorElement);
            }
        }

        public int getAbsolutePosition(boolean left) {
            return ResizeHandler.getAbsolutePosition(widget.getElement(), vertical, left);
        }

        // actual resize widget methods

        public FlexLayoutData getFlexLayoutData() {
            return ((WidgetLayoutData) widget.getLayoutData()).flex;
        }

        public void setNoFlex() {
            impl.setNoFlex(widget.getElement(), getFlexLayoutData(), vertical, isGrid());
        }

        public int getActualSize() {
            return impl.getActualSize(widget.getElement(), vertical);
        }

        // with borders / margins / paddings
        public int getFullSize() {
            return impl.getFullSize(widget.getElement(), vertical);
        }

        public void setFlex(double newFlex, GSize newPref) {
            impl.setFlex(getFlexLayoutData(), widget.getElement(), newFlex, newPref, vertical, isGrid());
        }

        // auto stretch / draw borders methods (+ getFlexLayoutData)

        public AlignmentLayoutData getAlignmentLayoutData() {
            if(widgets.size() == 1)
                return ((WidgetLayoutData)widgets.get(0).getLayoutData()).aligment;
            else
                return new AlignmentLayoutData(GFlexAlignment.STRETCH); // this virtual flex panel behaves
        }

        public Element getStretchElement() {
            return widget.getElement();
        }

        public void drawBorder(boolean set, boolean start, boolean vertical) {
            for(Widget widget : widgets)
                FlexPanel.drawBorder(widget, set, start, vertical);
        }

        public PanelParams updatePanels() {
            if(widgets.size() == 1)
                return FlexPanel.updatePanels(widgets.get(0));
            else {
                List<FlexStretchLine> virtualLines = new ArrayList<>();
                for (int i = 0, widgetsSize = widgets.size(); i < widgetsSize; i++) {
                    Widget widget = widgets.get(i);
                    AlignmentLayoutData alignmentLayoutData = ((WidgetLayoutData) widget.getLayoutData()).aligment;
                    FlexLayoutData flexLayoutData = gridLines.getLineLayoutData(i);
                    virtualLines.add(new FlexStretchLine() {
                        @Override
                        public FlexLayoutData getFlexLayoutData() {
                            return flexLayoutData;
                        }

                        @Override
                        public AlignmentLayoutData getAlignmentLayoutData() {
                            // in theory should be justify-items / justify-self, but for now it's stretch by default
                            return new AlignmentLayoutData(GFlexAlignment.STRETCH);
                        }

                        @Override
                        public Element getStretchElement() {
                            // since in both direction layout data is flex / stretch
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public PanelParams updatePanels() {
                            // actually we have virtual stretched flexPanel here:
                            // with single widget element, the as flexPanel direction and:
                            //      alignment = widget.alignment
                            //      flex = default "reversed alignment" (which is STRETCH by default, and can not be changed), i.e 1
                            // in that case the whole algorithms narrows down to alignment check, so for now we'll do it simple
                            PanelParams result = FlexPanel.updatePanels(widget);

                            GFlexAlignment alignment = alignmentLayoutData.baseAlignment;
                            if(!alignment.equals(GFlexAlignment.STRETCH)) {
                                InnerAlignment innerAlignment = new InnerFlexAlignment(alignment);
                                result = new PanelParams(result.top, result.bottom, result.left, result.right, result.hasBorders, vertical ? innerAlignment : result.horzAlignment, vertical ? result.vertAlignment : innerAlignment, result.vertCollapsed, result.empty);
                            }
                            return result;
                        }

                        @Override
                        public void drawBorder(boolean set, boolean start, boolean vertical) {
                            FlexPanel.drawBorder(widget, set, start, vertical);
                        }
                    });
                }
                return FlexPanel.updatePanels(true, !vertical, GFlexAlignment.START, virtualLines, false, false, false, null);
            }
        }
    }

    private static void drawBorder(Widget widget, boolean set, boolean start, boolean vertical) {
        if(vertical) {
            if(set) {
                if (start)
                    widget.addStyleName("topBorder");
                else
                    widget.addStyleName("bottomBorder");
            } else {
                if (start)
                    widget.removeStyleName("topBorder");
                else
                    widget.removeStyleName("bottomBorder");
            }
        } else {
            if(set) {
                if (start)
                    widget.addStyleName("leftBorder");
                else
                    widget.addStyleName("rightBorderNoWrap"); // assert !noWrap
            } else {
                if (start)
                    widget.removeStyleName("leftBorder");
                else
                    widget.removeStyleName("rightBorderNoWrap"); // assert !noWrap
            }
        }
    }

    public boolean isGrid() {
        return gridLines != null;
    }

    // filter visible (!)
    private List<FlexLine> getLines(Integer oppositePosition) {
        int lines = gridLines != null ? gridLines.getCount() : 1;

        List<FlexLine> result = new ArrayList<>();
        List<Widget> currentLine = new ArrayList<>();
        int currentLineSpan = 0;
        for(Widget widget : getChildren())
            if (widget.isVisible() &&
                    (!wrap || oppositePosition == null ||
                            ResizeHandler.getAbsolutePosition(widget.getElement(), !vertical, true) <= oppositePosition &&
                            ResizeHandler.getAbsolutePosition(widget.getElement(), !vertical, false) >= oppositePosition)) {
                int widgetSpan = ((WidgetLayoutData) widget.getLayoutData()).span;
                if (currentLineSpan > 0 && currentLineSpan + widgetSpan > lines) { // we need a new line
                    result.add(new FlexLine(currentLine));

                    currentLine = new ArrayList<>();
                    currentLineSpan = 0;
                }
                currentLine.add(widget);
                currentLineSpan += widgetSpan;
            }
        if(currentLineSpan > 0)
            result.add(new FlexLine(currentLine));

        return result;
    }

    private static class ParentSameFlexPanel {
        public final FlexPanel panel;
        public final List<FlexLine> lines;
        public final int index;

        public ParentSameFlexPanel(FlexPanel panel, List<FlexLine> lines, int index) {
            this.panel = panel;
            this.lines = lines;
            this.index = index;
        }
    }


    private ResizeHelper getResizeHelper(NativeEvent event) {
        int oppositePosition = ResizeHandler.getEventPosition(vertical, false, event);
        List<FlexLine> lines = getLines(oppositePosition);
        // optimization, lazy calculation
        Result<List<ParentSameFlexPanel>> parents = new Result<>();
        Function<Boolean, List<ParentSameFlexPanel>> lazyParents = onlyFirst -> {
            if(parents.result != null)
                return parents.result;

            List<ParentSameFlexPanel> result = new ArrayList<>();
            fillParentSameFlexPanels(vertical, result, oppositePosition, onlyFirst);
            if(!onlyFirst)
                parents.set(result);
            return result;
        };

        return new ResizeHelper() {
            @Override
            public int getChildCount() {
                return lines.size();
            }

            private FlexLine getChildLine(int index) {
                return lines.get(index);
            }

            @Override
            public void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement) {
                getChildLine(index).propagateChildResizeEvent(event, cursorElement);
            }

            @Override
            public int getChildAbsolutePosition(int index, boolean left) {
                return getChildLine(index).getAbsolutePosition(left);
            }

            @Override
            public double resizeChild(int index, int delta) {
                return resizeWidget(delta, lines, index, lazyParents.apply(false));
            }

            @Override
            public boolean isChildResizable(int index) {
                if (!isChildrenResizable(lines, index))
                    return false;

                // optimization, if it is the last element, and there is a "resizable" parent, we consider this element to be not resizable (assuming that this "resizable" parent will be resized)
                if (index == getChildCount() - 1 && !lazyParents.apply(true).isEmpty())
                    return false;

                return true;
            }

            @Override
            public boolean isVertical() {
                return vertical;
            }
        };
    }

    // the resize algorithm assumes that there should be flex column to the left, to make
    public boolean isChildrenResizable(List<FlexLine> lines, int widgetNumber) {
        if(!childrenResizable)
            return false;

        for(int i=widgetNumber;i>=0;i--) {
            if (lines.get(i).isFlex())
                return true;
        }
        return false;
    }

    // we need to guarantee somehow that resizing this parent container will lead to the same resizing of this container
    public void fillParentSameFlexPanels(boolean vertical, List<ParentSameFlexPanel> fillParents, int oppositePosition, boolean onlyFirst) {
//        if(1==1) return null;
        Widget parent = getParent();
        if(!(parent instanceof FlexPanel)) // it's some strange layouting, ignore it
            return;

        FlexPanel flexParent = (FlexPanel) parent;
        if(vertical != flexParent.vertical) {
            AlignmentLayoutData layoutData = ((WidgetLayoutData) getLayoutData()).aligment;
            if(layoutData.alignment == GFlexAlignment.STRETCH)
                flexParent.fillParentSameFlexPanels(vertical, fillParents, oppositePosition, onlyFirst);
        } else {
            List<FlexLine> lines = flexParent.getLines(oppositePosition);
            for (int i = 0; i < lines.size(); i++) {
                FlexLine line = lines.get(i);
                if (line.contains(this)) {
                    if(flexParent.isChildrenResizable(lines, i)) {
                        fillParents.add(new ParentSameFlexPanel(flexParent, lines, i));
                        if(!onlyFirst)
                            flexParent.fillParentSameFlexPanels(vertical, fillParents, oppositePosition, onlyFirst);
                    }
                    break;
                }
            }
        }
    }

    public double resizeWidget(double delta, List<FlexLine> lines, int lineNumber, List<ParentSameFlexPanel> parents) {
        List<FlexLine> children = lines;

        int size = children.size();
        double[] prefs = new double[size];
        double[] flexes = new double[size];

        int[] basePrefs = new int[size];
        double[] baseFlexes = new double[size];

        Element element = getElement();
        if(wrap) // in theory there should be recursive drop wrap, but on the other hand we'are dropping flex / shrink and flexbasis, so there will be no wrap inside
            impl.clearFlexWrap(element);

        int i = 0;
        for(FlexLine line : children) {
            FlexLayoutData layoutData = line.getFlexLayoutData();
            Double pxFlexBasis = layoutData.flexBasis != null ? layoutData.flexBasis.getResizeSize() : null;
            Integer pxBaseFlexBasis = layoutData.baseFlexBasis != null ? layoutData.baseFlexBasis.getIntResizeSize() : null;

            if (pxFlexBasis == null || pxBaseFlexBasis == null)
                line.setNoFlex();
            prefs[i] = pxFlexBasis != null ? pxFlexBasis : -1.0;
            flexes[i] = layoutData.flex;
            basePrefs[i] = pxBaseFlexBasis != null ? pxBaseFlexBasis : -1;
            baseFlexes[i] = layoutData.getBaseFlex();
            i++;
        }

        // we'll do it in different cycles to minimize the quantity of layouting
        int margins = 0;
        i = 0;
        for(FlexLine line : children) {
            int realSize = line.getActualSize(); // have no idea why not offset size is used
            if(prefs[i] < -0.5)
                prefs[i] = realSize;
            if(basePrefs[i] < 0)
                basePrefs[i] = realSize;
            margins += line.getFullSize() - realSize;
            i++;
        }

//        int body = ;
        // important to calculate viewWidth before setting new flexes
        ParentSameFlexPanel parentSameFlexPanel = parents.size() > 0 ? parents.get(0) : null;

        int viewWidth = impl.getActualSize(element, vertical) - margins;
        double restDelta = GwtClientUtils.calculateNewFlexes(lineNumber, delta, viewWidth, prefs, flexes, basePrefs, baseFlexes,  parentSameFlexPanel == null);

        if(parentSameFlexPanel != null && !GwtClientUtils.equals(restDelta, 0.0))
            restDelta = parentSameFlexPanel.panel.resizeWidget(restDelta, parentSameFlexPanel.lines, parentSameFlexPanel.index, parents.subList(1, parents.size()));

        if(wrap)
            impl.setFlexWrap(element);

        i = 0;
        for(FlexLine line : children) {
            FlexLayoutData layoutData = line.getFlexLayoutData();

            Integer newPref = (int) Math.round(prefs[i]);
            // if default (base) flex basis is auto and pref is equal to base flex basis, set flex basis to null (auto)
            if(newPref.equals(basePrefs[i]) && layoutData.baseFlexBasis == null)
                newPref = null;

            line.setFlex(flexes[i], GSize.getResizeNSize(newPref));
            i++;
        }

        onResize();

        return restDelta;
    }

    private static void setFlexModifier(boolean grid, boolean vertical, FlexLayoutData layoutData, FlexStretchLine w, FlexModifier modifier) {
        double prevFlex = layoutData.getFlex();
        GSize prevFlexBasis = layoutData.getFlexBasis();
        layoutData.flexModifier = modifier;
        double newFlex = layoutData.getFlex();
        GSize newFlexBasis = layoutData.getFlexBasis();
        if(!(prevFlex == newFlex && GwtClientUtils.nullEquals(prevFlexBasis, newFlexBasis))) // for optimization purposes + there might be problems with setBaseSize, since some data components use it explicitly without setting LayoutData
            impl.updateFlex(layoutData, w.getStretchElement(), vertical, grid);
    }

    private static void setStretchAlignment(boolean grid, boolean vertical, AlignmentLayoutData layoutData, FlexStretchLine line, boolean set) {
        if(!layoutData.baseAlignment.equals(GFlexAlignment.STRETCH)) {
            GFlexAlignment newAlignment = set ? GFlexAlignment.STRETCH : layoutData.baseAlignment;
            if(!newAlignment.equals(layoutData.alignment)) {
                layoutData.alignment = newAlignment;
                impl.updateAlignment(layoutData, line.getStretchElement(), vertical, grid);
            }
        }
    }

    public static void setGridAlignment(Element element, boolean vertical, GFlexAlignment alignment) {
        impl.setGridAlignment(alignment, element, vertical);
    }

    private interface InnerAlignment {

        InnerAlignment merge(InnerAlignment alignment);

        InnerAlignment DIFF = // alignment -> DIFF
                new InnerSystemAlignment("DIFF") {
            public InnerAlignment merge(InnerAlignment alignment) {
                return this;
            }
        };
        InnerAlignment ANY = new InnerSystemAlignment("ANY") {
            public InnerAlignment merge(InnerAlignment alignment) {
                return alignment;
            }
        };
    }

    private static abstract class InnerSystemAlignment implements InnerAlignment {

        private final String name;

        public InnerSystemAlignment(String name) {
            this.name = name;
        }
    }

    private static boolean isStretch(InnerAlignment inside, InnerAlignment outside) {
        return inside.merge(outside) != InnerAlignment.DIFF;
    }

    private static class InnerFlexAlignment implements InnerAlignment {

        private final GFlexAlignment flexAlignment;

        public InnerFlexAlignment(GFlexAlignment flexAlignment) {
            this.flexAlignment = flexAlignment;
            assert !flexAlignment.equals(GFlexAlignment.STRETCH);
        }

        public InnerAlignment merge(InnerAlignment alignment) {
            if(alignment instanceof InnerFlexAlignment)
                return flexAlignment.equals(((InnerFlexAlignment)alignment).flexAlignment) ? this : InnerAlignment.DIFF;
            return alignment.merge(this);
        }
    }

    private static class PanelParams {
        // borders
        public final boolean top;
        public final boolean bottom;
        public final boolean left;
        public final boolean right;

        public final boolean hasBorders; // we want to stretch only containers that has border a) for optimization purposes, b) there can be some problems with base components, and their explicit setBaseSize

        // inner alignment
        public final InnerAlignment horzAlignment;
        public final InnerAlignment vertAlignment;

        // collapsing
        public final boolean vertCollapsed;

        // empty
        public final boolean empty;

        public PanelParams(boolean top, boolean bottom, boolean left, boolean right, boolean hasBorders, InnerAlignment horzAlignment, InnerAlignment vertAlignment, boolean vertCollapsed, boolean empty) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;

            this.hasBorders = hasBorders;

            this.horzAlignment = horzAlignment;
            this.vertAlignment = vertAlignment;

            this.vertCollapsed = vertCollapsed;

            this.empty = empty;
        }
    }

    private interface FlexStretchLine {
        FlexLayoutData getFlexLayoutData();
        AlignmentLayoutData getAlignmentLayoutData();

        Element getStretchElement();

        PanelParams updatePanels();

        void drawBorder(boolean set, boolean start, boolean vertical);
    }

    public static PanelParams updatePanels(Widget widget) {
        // in a way similar to getParentSameFlexPanel (in web we assume that all intermediate panels are FlexPanel, in desktop we should do a recursion)
        if(widget instanceof FlexPanel) {
            FlexPanel flexPanel = (FlexPanel) widget;
            List<FlexLine> lines = flexPanel.getLines(null);
            boolean vertical = flexPanel.vertical;
            boolean grid = flexPanel.isGrid();
            boolean wrap = flexPanel.wrap;

            if(grid) {
                impl.setGridLines(flexPanel.getElement(), lines.size(), vertical);
                for (int i = 0, linesSize = lines.size(); i < linesSize; i++)
                    lines.get(i).setGridLines(i);
            }

            boolean collapsed = flexPanel instanceof CollapsiblePanel && ((CollapsiblePanel) flexPanel).collapsed;
            return updatePanels(grid, vertical, flexPanel.flexAlignment, lines, flexPanel instanceof CaptionPanel || flexPanel instanceof FlexTabbedPanel, !collapsed, collapsed, wrap ? flexPanel : null);
        } else
            return new PanelParams(widget instanceof TableContainer && ((TableContainer) widget).getTableComponent() instanceof DataGrid, false, false, false, false, InnerAlignment.DIFF, InnerAlignment.DIFF, false, false);
    }

    private static PanelParams updatePanels(boolean grid, boolean vertical, GFlexAlignment flexAlignment, List<? extends FlexStretchLine> lines, boolean forceTopBorders, boolean forceRestBorders, boolean forceVertCollapsed, FlexPanel wrapPanel) {
        boolean top = true, bottom = false, left = false, right = false;

        // STRETCH
        boolean hasBorders = false;

        InnerAlignment oppositeAlignment = InnerAlignment.ANY;
        boolean oppositeCollapsed = true; // COLLAPSE

        FlexStretchLine flexLine = null;
        FlexLayoutData flexLineLayoutData = null;
        boolean flexIs = false;

        InnerAlignment flexChildAlignment = null;
        int flexCount = 0;
        boolean flexCollapsed = false; // COLLAPSE

        boolean firstElement = true;
        boolean singleElement = true;

        // BORDERS
        // wrapped
        boolean drawWrapHorzBorder = false;
        Boolean prevWrapHorzBorder = null;
        // not wrapped
        Boolean prevBorder = null;
        FlexStretchLine prevLine = null;

        boolean empty = true;

        for (FlexStretchLine childLine : lines) {
            PanelParams childParams = childLine.updatePanels();

            // STRETCH
            FlexLayoutData flexLayoutData = childLine.getFlexLayoutData();
            AlignmentLayoutData alignmentLayoutData = childLine.getAlignmentLayoutData();

            GFlexAlignment childAlignment = alignmentLayoutData.baseAlignment;

            hasBorders |= childParams.hasBorders;

            // OPPOSITE direction

            // COLLAPSE
            boolean childOppositeCollapsed = vertical ? false : childParams.vertCollapsed;
            oppositeCollapsed = oppositeCollapsed && childOppositeCollapsed;
            // there is no need to change alignment, because in fact only flex change to 0 changes layouting

            InnerAlignment oppositeChildAlignment = vertical ? childParams.horzAlignment : childParams.vertAlignment;

            InnerAlignment innerChildAlignment;
            // if component is already stretched we're using the inner alignment
            if (childAlignment.equals(GFlexAlignment.STRETCH))
                innerChildAlignment = oppositeChildAlignment;
            else {
                innerChildAlignment = new InnerFlexAlignment(childAlignment);
                // opposite direction we can proceed immediately
                setStretchAlignment(grid, vertical, alignmentLayoutData, childLine, hasBorders && isStretch(oppositeChildAlignment, innerChildAlignment));
            }
            oppositeAlignment = oppositeAlignment.merge(innerChildAlignment);

            // MAIN direction

            // COLLAPSE
            boolean childMainCollapsed = vertical ? childParams.vertCollapsed : false;
            if(childMainCollapsed)
                flexCollapsed = true;

            setFlexModifier(grid, vertical, flexLayoutData, childLine, childMainCollapsed ? FlexModifier.COLLAPSE : null); // dropping STRETCH / setting COLLAPSE

            boolean childMainFlex = flexLayoutData.isFlex();
            if (firstElement || flexAlignment.equals(GFlexAlignment.START)) {
                flexLine = childLine;
                flexLineLayoutData = flexLayoutData;
                flexIs = childMainFlex;
                flexChildAlignment = vertical ? childParams.vertAlignment : childParams.horzAlignment;
            }
            if (childMainFlex)
                flexCount++;

            // BORDERS
            if(childParams.empty) // just ignoring empty containers to avoid borders around them
                continue;

            empty = false;

            if(wrapPanel != null) { // wrapped
                if(prevWrapHorzBorder != null)
                    drawWrapHorzBorder |= prevWrapHorzBorder || childParams.left;
                prevWrapHorzBorder = childParams.right;

                left |= childParams.left;
                right |= childParams.right;
                top &= childParams.top;
                bottom |= childParams.bottom;
            } else { // not wrapped
                boolean drawBorder = false;
                if (vertical) {
                    left |= childParams.left;
                    right |= childParams.right;

                    if (prevBorder != null)
                        drawBorder = prevBorder && !childParams.top;
                    else
                        top = childParams.top;
                } else {
                    top &= childParams.top;
                    bottom |= childParams.bottom;

                    if (prevBorder != null)
                        drawBorder = prevBorder || childParams.left;
                    else
                        left = childParams.left;
                }

                childLine.drawBorder(false, false, vertical);
                childLine.drawBorder(drawBorder, true, vertical);
                if (drawBorder)
                    prevLine.drawBorder(true, false, vertical);

                prevLine = childLine;

                if(vertical)
                    prevBorder = childParams.bottom;
                else
                    prevBorder = childParams.right;
            }

            if (firstElement)
                singleElement = false;
            firstElement = false;
        }

        if(wrapPanel != null) { // in grid wrap should be implemented as auto-fit (however it's possible only without align-captions)
            wrapPanel.removeStyleName("flexHorzBorderWrap");
            if(drawWrapHorzBorder)
                wrapPanel.addStyleName("flexHorzBorderWrap");

            for (int i = 1, linesSize = lines.size(); i < linesSize; i++)
                lines.get(i).drawBorder(drawWrapHorzBorder, true, false);
            // maybe something should be done for vertical direction (bottom, top), but there is no obvious heuristics for that
        } else {
            if(prevBorder != null) {
                if (vertical)
                    bottom = prevBorder;
                else
                    right = prevBorder;
            }
        }

        InnerAlignment mainAlignment = InnerAlignment.ANY;
        boolean mainCollapsed = false;

        if(!empty) {
            if (flexCount == 0 || flexCount == 1 && flexIs) { // if we have no stretched or only one stretched element (that we would stretch anyway)
                if (flexCount == 0) {
                    // COLLAPSE
                    if(flexCollapsed) // no flex elements left, but there are collapsed elements, we're collapsing this container
                        mainCollapsed = true;

                    if (singleElement || !flexAlignment.equals(GFlexAlignment.CENTER)) { // we cannot stretch center element when there are several elements (it will break the centering)
                        mainAlignment = new InnerFlexAlignment(flexAlignment);

                        if (isStretch(flexChildAlignment, mainAlignment) && hasBorders && !flexCollapsed) // !flexCollapsed is important because modifier also changes flexBasis
                            setFlexModifier(grid, vertical, flexLineLayoutData, flexLine, FlexModifier.STRETCH);
                    }
                } else // single already stretched element we're using the inner alignment
                    mainAlignment = flexChildAlignment;
            } else
                mainAlignment = InnerAlignment.DIFF;
        } else // empty, however it seems that it's needed only for columns container (which can have no children but still be visible)
            top = false;

        InnerAlignment horzAlignment = vertical ? oppositeAlignment : mainAlignment;
        InnerAlignment vertAlignment = vertical ? mainAlignment : oppositeAlignment;

        boolean vertCollapsed = vertical ? mainCollapsed : oppositeCollapsed;

        if (forceTopBorders) {
            top = hasBorders = left = right = true;
            if(forceRestBorders)
                bottom = true;
        }

        if (forceVertCollapsed)
            vertCollapsed = true;

        return new PanelParams(top, bottom, left, right, hasBorders, horzAlignment, vertAlignment, vertCollapsed, empty);
    }

    @Override
    public void setPreferredSize(boolean set, Result<Integer> grids) {
        for(Widget widget : getChildren()) {
            if(widget.isVisible()) {
                // main direction (dropping size / shrink)
                FlexLayoutData flex = ((WidgetLayoutData) widget.getLayoutData()).flex;
                if (flex.baseFlexBasis != null || flex.shrink)
                    impl.setPreferredSize(set, widget.getElement(), flex, vertical, isGrid());

                // opposite direction (dropping size / shrink)
                SizedFlexPanel.setIntrinisticPreferredWidth(set, widget);

                if (widget instanceof HasMaxPreferredSize)
                    ((HasMaxPreferredSize) widget).setPreferredSize(set, grids);
            }
        }
    }

    public void setChildPreferredSize(boolean set, Widget widget) {
        // main direction (dropping size / shrink)
        FlexLayoutData flex = ((WidgetLayoutData) widget.getLayoutData()).flex;
        if (flex.baseFlexBasis != null || flex.shrink)
            impl.setPreferredSize(set, widget.getElement(), flex, vertical, isGrid());

        // opposite direction (dropping size / shrink)
        SizedFlexPanel.setIntrinisticPreferredWidth(set, widget);
    }

    @Override
    protected void add(Widget child, Element container) {
        assert false;
        super.add(child, container);
    }

    @Override
    protected void insert(Widget child, Element container, int beforeIndex, boolean domInsert) {
        assert false;
        super.insert(child, container, beforeIndex, domInsert);
    }
}
