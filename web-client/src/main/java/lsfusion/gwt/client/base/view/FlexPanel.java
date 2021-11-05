package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.resize.ResizeHelper;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.design.view.flex.CaptionContainerHolder;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.calculateStackMaxPreferredSize;

public class FlexPanel extends ComplexPanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize {

    protected static FlexPanelImpl impl = FlexPanelImpl.get();

    private final DivElement parentElement;

    private final boolean vertical;

    private GFlexAlignment flexAlignment;

    private boolean visible = true;

    public boolean childrenResizable = true;

    public FlexPanel() {
        this(false);
    }

    public FlexPanel(boolean vertical) {
        this(vertical, GFlexAlignment.START);
    }

    public void setFlexAlignment(GFlexAlignment flexAlignment) {
        this.flexAlignment = flexAlignment;
        assert !flexAlignment.equals(GFlexAlignment.STRETCH);
    }

    private FlexLayoutData[] gridLines;
    
    public FlexPanel(boolean vertical, GFlexAlignment flexAlignment) {
        this(vertical, flexAlignment, null);
    }

    public FlexPanel(boolean vertical, GFlexAlignment flexAlignment, FlexLayoutData[] gridLines) {
        this.vertical = vertical;

        this.gridLines = gridLines;

        this.flexAlignment = flexAlignment;

        parentElement = Document.get().createDivElement();

        impl.setupParentDiv(parentElement, vertical, gridLines, flexAlignment);

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
            impl.setVisible(parentElement, visible, isGrid());
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
        add(widget, beforeIndex, alignment, 0, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addFill(Widget widget) {
        addFill(widget, getWidgetCount());
    }

    // we want to have the same behaviour as STRETCH : as flex-basis is 0, but auto size is relevant
    // in theory this can be substituted with if parent layout is autosized ? null : 0, but sometimes we don't know parent layout
    public static void setFillShrink(Widget widget) {
        widget.getElement().getStyle().setProperty("flexShrink", "1");
    }
    public void addFillFlex(Widget widget, Integer flexBasis) {
        addFill(widget, getWidgetCount(), flexBasis);
    }

    public void addFill(Widget widget, GFlexAlignment alignment) {
        add(widget, getWidgetCount(), alignment, 1, null);
    }
    public void addFill(Widget widget, int beforeIndex, Integer flexBasis) {
        add(widget, beforeIndex, GFlexAlignment.STRETCH, 1, flexBasis);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, Integer flexBasis) {
        // Detach new child.
        widget.removeFromParent();

        // Logical attach.
        getChildren().insert(widget, beforeIndex);

        // Physical attach.
        Element childElement = widget.getElement();

        WidgetLayoutData layoutData = impl.insertChild(parentElement, childElement, beforeIndex, alignment, flex, flexBasis, vertical, isGrid());
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
        addFill(widget, beforeIndex, vertical ? null : 0);
    }

    // we're setting min-width/height and not width/height for two reasons:
    // a) alignment STRETCH doesn't work when width is set (however for the alignment other than STRETCH min param works as max of this min size, and auto size, and this behaviour is different from flex:0 0 size what we want to get)
    // b) flexBasis auto doesn't respect flexBasis of its descendants (!!! it's not true for vertical direction, see addFill comment !!!), but respects min-width (however with that approach in future there might be some problems with flex-shrink if we we'll want to support it)
    public static void setBaseSize(Widget widget, boolean vertical, Integer size) {
        setBaseSize(widget, vertical, size, false);
    }
    public static void setBaseSize(Widget widget, boolean vertical, Integer size, boolean oppositeAndFixed) {
        setBaseSize(widget.getElement(), vertical, size, oppositeAndFixed);
    }
    public static void setBaseSize(Element element, boolean vertical, Integer size, boolean oppositeAndFixed) {
        String propName = vertical ? (oppositeAndFixed ? "height" : "minHeight") : (oppositeAndFixed ? "width" : "minWidth");
        if(size != null)
            element.getStyle().setProperty(propName, size + "px");
        else
            element.getStyle().clearProperty(propName);
    }

    public void setChildFlexBasis(Widget w, int flexBasis, boolean grid) {
        impl.setFlexBasis(((WidgetLayoutData) w.getLayoutData()).flex, w.getElement(), flexBasis, vertical, grid);
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
        ResizeHandler.checkResizeEvent(resizeHelper, cursorElement, null, event);
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return calculateStackMaxPreferredSize(this.iterator(), isVertical());
    }

    public static final class FlexLayoutData {

        // current, both are changed in resizeWidget
        public double flex; // also changed in setStretchFlex
        public Integer flexBasis; // changed on autosize and on tab change (in this case baseFlexBasis should be change to)

        public double baseFlex;
        public Integer baseFlexBasis; // if null, than we have to get it similar to fixFlexBases by setting flexes to 0

        public Integer gridLine; // only for "main" widgets in column should be set

        public boolean isFlex() {
            return baseFlex > 0;
        }

        public boolean isAutoSized() {
            return baseFlexBasis == null;
        }

        public void setFlexBasis(Integer flexBasis) {
            this.flexBasis = flexBasis;
            baseFlexBasis = flexBasis;
        }

        public FlexLayoutData(double flex, Integer flexBasis) {
            this.flex = flex;
            this.baseFlex = flex;

            this.flexBasis = flexBasis;
            baseFlexBasis = flexBasis;
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

        public void setAutoSizeFlex() {
            impl.setAutoSizeFlex(widget.getElement(), getFlexLayoutData(), vertical, isGrid());
        }

        public int getSize() {
            return impl.getSize(widget.getElement(), vertical);
        }

        // with borders / margins / paddings
        public int getFullSize() {
            return impl.getFullSize(widget.getElement(), vertical);
        }

        public void setFlex(double newFlex, Integer newPref) {
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

        public DrawBorders autoStretchAndDrawBorders() {
            if(widgets.size() == 1)
                return FlexPanel.autoStretchAndDrawBorders(widgets.get(0));
            else {
                List<FlexStretchLine> virtualLines = new ArrayList<>();
                for (int i = 0, widgetsSize = widgets.size(); i < widgetsSize; i++) {
                    Widget widget = widgets.get(i);
                    FlexLayoutData flexLayoutData = gridLines[i];
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
                        public DrawBorders autoStretchAndDrawBorders() {
                            // actually we have virtual stretched flexPanel here but with the opposite direction
                            // however such containers should be transparent
                            return FlexPanel.autoStretchAndDrawBorders(widget);
                        }

                        @Override
                        public void drawBorder(boolean set, boolean start, boolean vertical) {
                            FlexPanel.drawBorder(widget, set, start, vertical);
                        }
                    });
                }
                return FlexPanel.autoStretchAndDrawBorders(true, !vertical, GFlexAlignment.START, virtualLines, false);
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
                    widget.addStyleName("rightBorder");
            } else {
                if (start)
                    widget.removeStyleName("leftBorder");
                else
                    widget.removeStyleName("rightBorder");
            }
        }
    }

    private boolean isGrid() {
        return gridLines != null;
    }

    // filter visible (!)
    private List<FlexLine> getLines() {
        int lines = gridLines != null ? gridLines.length : 1;

        List<FlexLine> result = new ArrayList<>();
        List<Widget> currentLine = new ArrayList<>();
        int currentLineSpan = 0;
        for(Widget widget : getChildren())
            if(widget.isVisible()) {
                int widgetSpan = ((WidgetLayoutData)widget.getLayoutData()).span;
                if(currentLineSpan + widgetSpan > lines) { // we need a new line
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

    private ResizeHelper resizeHelper = new ResizeHelper() {
        @Override
        public int getChildCount() {
            return getLines().size();
        }

        private FlexLine getChildLine(int index) {
            return getLines().get(index);
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
        public void resizeChild(int index, int delta) {
            resizeWidget(index, delta);
        }

        @Override
        public boolean isChildResizable(int index) {
            if(!isChildrenResizable(index))
                return false;

            // optimization, if it is the last element, and there is a "resizable" parent, we consider this element to be not resizable (assuming that this "resizable" parent will be resized)
            if(index == getChildCount() - 1 && getParentSameFlexPanel(vertical) != null)
                return false;

            return true;
        }

        @Override
        public boolean isVertical() {
            return vertical;
        }
    };

    // the resize algorithm assumes that there should be flex column to the right, to make
    public boolean isChildrenResizable(int widgetNumber) {
        if(!childrenResizable)
            return false;

        List<FlexLine> lines = getLines();
        for(int i=widgetNumber;i>=0;i--) {
            if (lines.get(i).isFlex())
                return true;
        }
        return false;
    }

    // we need to guarantee somehow that resizing this parent container will lead to the same resizing of this container
    public Pair<FlexPanel, Integer> getParentSameFlexPanel(boolean vertical) {
//        if(1==1) return null;
        Widget parent = getParent();
        if(!(parent instanceof FlexPanel)) // it's some strange layouting, ignore it
            return null;

        FlexPanel flexParent = (FlexPanel) parent;
        if(vertical != flexParent.vertical) {
            AlignmentLayoutData layoutData = ((WidgetLayoutData) getLayoutData()).aligment;
            if(layoutData.alignment == GFlexAlignment.STRETCH)
                return flexParent.getParentSameFlexPanel(vertical);
        } else {
            List<FlexLine> lines = flexParent.getLines();
            for (int i = 0; i < lines.size(); i++) {
                FlexLine line = lines.get(i);
                if (line.contains(this)) {
                    if(flexParent.isChildrenResizable(i))
                        return new Pair<>(flexParent, i);
                    break;
                }
            }
        }
        return null;
    }

    public void resizeWidget(int lineNumber, double delta) {
        Pair<FlexPanel, Integer> parentSameFlexPanel = getParentSameFlexPanel(vertical);

        List<FlexLine> children = getLines();

        int size = children.size();
        double[] prefs = new double[size];
        double[] flexes = new double[size];

        int[] basePrefs = new int[size];
        double[] baseFlexes = new double[size];

        int i = 0;
        for(FlexLine line : children) {
            FlexLayoutData layoutData = line.getFlexLayoutData();
            if (layoutData.flexBasis == null || layoutData.baseFlexBasis == null)
                line.setAutoSizeFlex();
            if (layoutData.flexBasis != null)
                prefs[i] = layoutData.flexBasis;
            flexes[i] = layoutData.flex;
            if (layoutData.baseFlexBasis != null)
                basePrefs[i] = layoutData.baseFlexBasis;
            baseFlexes[i] = layoutData.baseFlex;
            i++;
        }

        // we'll do it in different cycles to minimize the quantity of layouting
        int margins = 0;
        i = 0;
        for(FlexLine line : children) {
            FlexLayoutData layoutData = line.getFlexLayoutData();

            int realSize = line.getSize(); // calculating size
            if(layoutData.flexBasis == null || layoutData.baseFlexBasis == null) {
                if(layoutData.flexBasis == null)
                    prefs[i] = realSize;
                if(layoutData.baseFlexBasis == null)
                    basePrefs[i] = realSize;
            }
            margins += line.getFullSize() - realSize;
            i++;
        }

//        int body = ;
        // important to calculate viewWidth before setting new flexes
        int viewWidth = impl.getSize(getElement(), vertical) - margins;
        double restDelta = GwtClientUtils.calculateNewFlexes(lineNumber, delta, viewWidth, prefs, flexes, basePrefs, baseFlexes,  parentSameFlexPanel == null);

        if(parentSameFlexPanel != null && !GwtClientUtils.equals(restDelta, 0.0))
            parentSameFlexPanel.first.resizeWidget(parentSameFlexPanel.second, restDelta);

        i = 0;
        for(FlexLine line : children) {
            FlexLayoutData layoutData = line.getFlexLayoutData();

            Integer newPref = (int) Math.round(prefs[i]);
            // if default (base) flex basis is auto and pref is equal to base flex basis, set flex basis to null (auto)
            if(newPref.equals(basePrefs[i]) && layoutData.baseFlexBasis == null)
                newPref = null;

            line.setFlex(flexes[i], newPref);
            i++;
        }

        onResize();
    }

    private static void setStretchFlex(boolean grid, boolean vertical, FlexLayoutData layoutData, FlexStretchLine w, boolean set) {
        if(!layoutData.isFlex()) {
            double newFlex = set ? 1 : layoutData.baseFlex;
            if(layoutData.flex != newFlex) { // for optimization purposes + there might be problems with setBaseSize, since some data components use it explicitly without setting LayoutData
                layoutData.flex = newFlex;
                impl.updateFlex(layoutData, w.getStretchElement(), vertical, grid);
            }
        }
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

    private static class DrawBorders {
        public final boolean top;
        public final boolean bottom;
        public final boolean left;
        public final boolean right;

        public final boolean hasBorders; // we want to stretch only containers that has border a) for optimization purposes, b) there can be some problems with base components, and their explicit setBaseSize
        // null if not applicable
        public final GFlexAlignment horzAlignment;
        public final GFlexAlignment vertAlignment;

        public DrawBorders(boolean top, boolean bottom, boolean left, boolean right, boolean hasBorders, GFlexAlignment horzAlignment, GFlexAlignment vertAlignment) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;

            this.hasBorders = hasBorders;
            this.horzAlignment = horzAlignment;
            assert horzAlignment == null || !horzAlignment.equals(GFlexAlignment.STRETCH);
            this.vertAlignment = vertAlignment;
            assert vertAlignment == null || !vertAlignment.equals(GFlexAlignment.STRETCH);
        }
    }

    private interface FlexStretchLine {
        FlexLayoutData getFlexLayoutData();
        AlignmentLayoutData getAlignmentLayoutData();

        Element getStretchElement();

        DrawBorders autoStretchAndDrawBorders();

        void drawBorder(boolean set, boolean start, boolean vertical);
    }

    public static DrawBorders autoStretchAndDrawBorders(Widget widget) {
        // in a way similar to getParentSameFlexPanel (in web we assume that all intermediate panels are FlexPanel, in desktop we should do a recursion)
        if(widget instanceof FlexPanel) {
            FlexPanel flexPanel = (FlexPanel) widget;
            List<FlexLine> lines = flexPanel.getLines();
            boolean vertical = flexPanel.vertical;
            boolean grid = flexPanel.isGrid();

            if(grid) {
                impl.setGridLines(flexPanel.getElement(), lines.size(), vertical);
                for (int i = 0, linesSize = lines.size(); i < linesSize; i++)
                    lines.get(i).setGridLines(i);
            }

            return autoStretchAndDrawBorders(grid, vertical, flexPanel.flexAlignment, lines, flexPanel instanceof CaptionPanel || flexPanel instanceof FlexTabbedPanel);
        } else
            return new DrawBorders(widget instanceof ResizableSimplePanel && ((ResizableSimplePanel) widget).getWidget() instanceof DataGrid, false, false, false, false, null, null);
    }

    private static DrawBorders autoStretchAndDrawBorders(boolean grid, boolean vertical, GFlexAlignment flexAlignment, List<? extends FlexStretchLine> lines, boolean forceBorders) {
        boolean top = true, bottom = false, left = false, right = false;

        boolean hasBorders = false;

        GFlexAlignment oppositeAlignment = null;

        Boolean prevBorder = null;
        FlexStretchLine prevLine = null;

        FlexStretchLine flexLine = null;
        FlexLayoutData flexLineLayoutData = null;
        boolean flexIs = false;
        GFlexAlignment flexChildAlignment = null;
        int flexCount = 0;
        boolean singleElement = true;

        for (FlexStretchLine childLine : lines) {
            DrawBorders childBorders = childLine.autoStretchAndDrawBorders();
            if (childBorders != null) {
                FlexLayoutData flexLayoutData = childLine.getFlexLayoutData();
                AlignmentLayoutData alignmentLayoutData = childLine.getAlignmentLayoutData();

                GFlexAlignment childAlignment = alignmentLayoutData.baseAlignment;

                hasBorders |= childBorders.hasBorders;

                // OPPOSITE direction
                GFlexAlignment oppositeChildAlignment = vertical ? childBorders.horzAlignment : childBorders.vertAlignment;
                // opposite direction we can proceed immediately (and before changing childAlignment see below)
                setStretchAlignment(grid, vertical, alignmentLayoutData, childLine, hasBorders && oppositeChildAlignment != null && oppositeChildAlignment.equals(childAlignment));
                // if component is already stretched we're using the inner alignment
                if(childAlignment.equals(GFlexAlignment.STRETCH))
                    childAlignment = oppositeChildAlignment;
                if(prevBorder == null) // first visible
                    oppositeAlignment = childAlignment;
                else {
                    singleElement = false;

                    if(oppositeAlignment != null && (childAlignment == null || !oppositeAlignment.equals(childAlignment)))
                        oppositeAlignment = null;
                }

                // MAIN direction
                boolean childMainFlex = flexLayoutData.isFlex();
                if(prevBorder == null || flexAlignment.equals(GFlexAlignment.START)) {
                    flexLine = childLine;
                    flexLineLayoutData = flexLayoutData;
                    flexIs = childMainFlex;
                    flexChildAlignment = vertical ? childBorders.vertAlignment : childBorders.horzAlignment;
                }
                if(childMainFlex)
                    flexCount++;
                setStretchFlex(grid, vertical, flexLayoutData, childLine, false);

                childLine.drawBorder(false, false, vertical);
                boolean drawBorder = false;

                // drawing borders
                if (vertical) {
                    left |= childBorders.left;
                    right |= childBorders.right;

                    if (prevBorder != null)
                        drawBorder = prevBorder && !childBorders.top;
                    else
                        top = childBorders.top;

                    prevBorder = childBorders.bottom;
                } else {
                    top &= childBorders.top;
                    bottom |= childBorders.bottom;

                    if (prevBorder != null)
                        drawBorder = prevBorder || childBorders.left;
                    else
                        left = childBorders.left;

                    prevBorder = childBorders.right;
                }

                childLine.drawBorder(drawBorder, true, vertical);
                if(drawBorder)
                    prevLine.drawBorder(true, false, vertical);

                prevLine = childLine;
            }
        }

        if (prevBorder == null) // invisible, however it seems that it's needed only for columns container
            return null;

        if (vertical)
            bottom = prevBorder;
        else
            right = prevBorder;

        GFlexAlignment mainAlignment = null;
        if(flexCount == 0 || flexCount == 1 && flexIs) { // if we have no stretched or only one stretched element (that we would stretch anyway)
            if(flexCount == 0) {
                if (singleElement || !flexAlignment.equals(GFlexAlignment.CENTER)) { // we cannot stretch center element when there are several elements (it will break the centering)
                    if (flexChildAlignment != null && flexChildAlignment.equals(flexAlignment) && hasBorders)
                        setStretchFlex(grid, vertical, flexLineLayoutData, flexLine, true);

                    mainAlignment = flexAlignment;
                }
            } else // single already stretched element we're using the inner alignment
                mainAlignment = flexChildAlignment;
        }

        GFlexAlignment horzAlignment = vertical ? oppositeAlignment : mainAlignment;
        GFlexAlignment vertAlignment = vertical ? mainAlignment : oppositeAlignment;

        if (forceBorders)
            top = left = right = bottom = hasBorders = true;

        return new DrawBorders(top, bottom, left, right, hasBorders, horzAlignment, vertAlignment);
    }
}
