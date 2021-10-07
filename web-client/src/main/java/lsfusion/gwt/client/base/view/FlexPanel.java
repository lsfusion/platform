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
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.calculateStackMaxPreferredSize;

public class FlexPanel extends ComplexPanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize {

    protected static FlexPanelImpl impl = FlexPanelImpl.get();

    private final DivElement parentElement;

    private final boolean vertical;

    private final GFlexAlignment flexAlignment;

    private boolean visible = true;

    public boolean childrenResizable = true;

    public FlexPanel() {
        this(false);
    }

    public FlexPanel(boolean vertical) {
        this(vertical, GFlexAlignment.START);
    }

    public FlexPanel(boolean vertical, GFlexAlignment flexAlignment) {
        this.vertical = vertical;

        this.flexAlignment = flexAlignment;
        assert !flexAlignment.equals(GFlexAlignment.STRETCH);

        parentElement = Document.get().createDivElement();

        impl.setupParentDiv(parentElement, vertical, flexAlignment);

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
            impl.setVisible(parentElement, visible);
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
    public void addFillStretch(Widget widget) {
        addFillFlex(widget, null);
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

        LayoutData layoutData = impl.insertChild(parentElement, childElement, beforeIndex, alignment, flex, flexBasis, vertical);
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

    public void setChildFlexBasis(Widget w, int flexBasis) {
        impl.setFlexBasis((LayoutData) w.getLayoutData(), w.getElement(), flexBasis, vertical);
    }

    @Override
    public boolean remove(Widget w) {
        boolean removed = super.remove(w);
        if (removed) {
            impl.removeChild((LayoutData) w.getLayoutData());
        }
        return removed;
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

    public static final class LayoutData {
        public Element child;
        public GFlexAlignment alignment;

        // current, both are changed in resizeWidget
        public double flex;
        public Integer flexBasis; // changed on autosize and on tab change (in this case baseFlexBasis should be change to)

        public double baseFlex;
        public Integer baseFlexBasis; // if null, than we have to get it similar to fixFlexBases by setting flexes to 0

        public final GFlexAlignment baseAlignment;

        public boolean isFlex() {
            return baseFlex > 0;
        }

        public void setFlexBasis(Integer flexBasis) {
            this.flexBasis = flexBasis;
            baseFlexBasis = flexBasis;
        }

        public LayoutData(Element child, GFlexAlignment alignment, double flex, Integer flexBasis) {
            this.child = child;
            this.alignment = alignment;
            this.flex = flex;
            this.flexBasis = flexBasis;

            this.baseFlex = flex;
            this.baseFlexBasis = flexBasis;
            this.baseAlignment = alignment;
        }
    }

    private ResizeHelper resizeHelper = new ResizeHelper() {
        @Override
        public int getChildCount() {
            return getChildren().size();
        }

        @Override
        public Element getChildElement(int index) {
            return getChildren().get(index).getElement();
        }

        @Override
        public Widget getChildWidget(int index) {
            return getChildren().get(index);
        }

        @Override
        public void resizeChild(int index, int delta) {
            resizeWidget(index, delta);
        }
        @Override
        public boolean isChildResizable(int index) {
            if(!isWidgetResizable(index))
                return false;

            // optimization, if it is the last element, and there is a "resizable" parent, we consider this element to be not resizable (assuming that this "resizable" parent will be resized)
            if(index == getChildren().size() - 1 && getParentSameFlexPanel(vertical) != null)
                return false;

            return true;
        }

        @Override
        public boolean isChildVisible(int index) {
            return getChildren().get(index).isVisible();
        }

        @Override
        public boolean isVertical() {
            return vertical;
        }
    };

    // the resize algorithm assumes that there should be flex column to the right, to make
    public boolean isWidgetResizable(int widgetNumber) {
        if(!childrenResizable)
            return false;

        WidgetCollection children = getChildren();
        for(int i=widgetNumber;i>=0;i--) {
            Widget child = children.get(i);
            LayoutData layoutData = (LayoutData) child.getLayoutData();
            if(layoutData.isFlex() && child.isVisible())
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
            LayoutData layoutData = (LayoutData) getLayoutData();
            if(layoutData.alignment == GFlexAlignment.STRETCH)
                return flexParent.getParentSameFlexPanel(vertical);
        } else {
            int index = flexParent.getChildren().indexOf(this);
            if(flexParent.isWidgetResizable(index))
                return new Pair<>(flexParent, index);
        }
        return null;
    }

    private Widget getInnerChild(int innerPosition) {
        WidgetCollection children = getChildren();
        for(Widget child : children)
            if(child.isVisible()) {
                Element element = child.getElement();
                if(ResizeHandler.getAbsolutePosition(vertical, element, false) >= innerPosition)
                    return child;
            }
        return null;
    }

    public void resizeWidget(int widgetNumber, double delta) {
        Pair<FlexPanel, Integer> parentSameFlexPanel = getParentSameFlexPanel(vertical);

        List<Widget> children = new ArrayList<>();
        int i = 0;
        int invisibleBefore = 0;
        for(Widget child : getChildren()) {
            if (child.isVisible())
                children.add(child);
            else if(i < widgetNumber) {
                invisibleBefore++;
            }
            i++;
        }
        widgetNumber -= invisibleBefore;

        int size = children.size();
        double[] prefs = new double[size];
        double[] flexes = new double[size];

        int[] basePrefs = new int[size];
        double[] baseFlexes = new double[size];

        i = 0;
        for(Widget widget : children) {
            LayoutData layoutData = (LayoutData) widget.getLayoutData();
            if (layoutData.flexBasis == null || layoutData.baseFlexBasis == null)
                impl.setFlex(widget.getElement(), 0, null, vertical);
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
        for(Widget widget : children) {
            LayoutData layoutData = (LayoutData) widget.getLayoutData();
            Element element = widget.getElement();

            int realSize = impl.getSize(element, vertical); // calculating size
            if(layoutData.flexBasis == null || layoutData.baseFlexBasis == null) {
                if(layoutData.flexBasis == null)
                    prefs[i] = realSize;
                if(layoutData.baseFlexBasis == null)
                    basePrefs[i] = realSize;
            }
            margins += impl.getFullSize(element, vertical) - realSize;
            i++;
        }

//        int body = ;
        // important to calculate viewWidth before setting new flexes
        int viewWidth = impl.getSize(getElement(), vertical) - margins;
        double restDelta = GwtClientUtils.calculateNewFlexes(widgetNumber, delta, viewWidth, prefs, flexes, basePrefs, baseFlexes,  parentSameFlexPanel == null);

        if(parentSameFlexPanel != null && !GwtClientUtils.equals(restDelta, 0.0))
            parentSameFlexPanel.first.resizeWidget(parentSameFlexPanel.second, restDelta);

        i = 0;
        for(Widget widget : children) {
            LayoutData layoutData = (LayoutData) widget.getLayoutData();

            Integer newPref = (int) Math.round(prefs[i]);
            // if default (base) flex basis is auto and pref is equal to base flex basis, set flex basis to null (auto)
            if(newPref.equals(basePrefs[i]) && layoutData.baseFlexBasis == null)
                newPref = null;
            impl.setFlex(layoutData, widget.getElement(), flexes[i], newPref, vertical);

            i++;
        }

        onResize();
    }

    private void setStretchFlex(Widget w, boolean set) {
        impl.setStretchFlex((LayoutData) w.getLayoutData(), w.getElement(), set, vertical);
    }

    private void setStretchAlignment(Widget w, boolean set) {
        impl.setStretchAlignment((LayoutData) w.getLayoutData(), w.getElement(), set, vertical);
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

    public static DrawBorders autoStretchAndDrawBorders(Widget widget) {
        boolean top = false, bottom = false, left = false, right = false;

        GFlexAlignment horzAlignment = null, vertAlignment = null;
        boolean hasBorders = false;

        // in a way similar to getParentSameFlexPanel (in web we assume that all intermediate panels are FlexPanel, in desktop we should do a recursion)
        if(widget instanceof FlexPanel) {
            Boolean prevBorder = null;
            Widget prevWidget = null;

            FlexPanel flexPanel = (FlexPanel) widget;
            int childCount = flexPanel.getWidgetCount();

            GFlexAlignment flexAlignment = flexPanel.flexAlignment;
            GFlexAlignment oppositeAlignment = null;

            Widget flexWidget = null;
            boolean flexIs = false;
            GFlexAlignment flexChildAlignment = null;
            int flexCount = 0;
            boolean singleElement = true;

            boolean vertical = flexPanel.vertical;
            for (int i = 0; i < childCount; i++) {
                Widget childWidget = flexPanel.getWidget(i);
                // we need to exclude absolutely positioned elements, since they are not used in flex layouting
                if (childWidget.isVisible() && (!(flexPanel instanceof CaptionPanel && childWidget == ((CaptionPanel) flexPanel).legend))) {
                    DrawBorders childBorders = autoStretchAndDrawBorders(childWidget);
                    if (childBorders != null) {
                        LayoutData layoutData = (LayoutData) childWidget.getLayoutData();
                        GFlexAlignment childAlignment = layoutData.baseAlignment;

                        hasBorders |= childBorders.hasBorders;

                        // OPPOSITE direction
                        GFlexAlignment oppositeChildAlignment = vertical ? childBorders.horzAlignment : childBorders.vertAlignment;
                        // opposite direction we can proceed immediately (and before chaging childAligment see below)
                        flexPanel.setStretchAlignment(childWidget, hasBorders && oppositeChildAlignment != null && oppositeChildAlignment.equals(childAlignment));
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
                        boolean childMainFlex = layoutData.isFlex();
                        if(prevBorder == null || flexAlignment.equals(GFlexAlignment.START)) {
                            flexWidget = childWidget;
                            flexIs = childMainFlex;
                            flexChildAlignment = vertical ? childBorders.vertAlignment : childBorders.horzAlignment;
                        }
                        if(childMainFlex)
                            flexCount++;
                        flexPanel.setStretchFlex(childWidget, false);

                        // drawing borders
                        if (vertical) {
                            left |= childBorders.left;
                            right |= childBorders.right;

                            childWidget.removeStyleName("topBorder");
                            childWidget.removeStyleName("bottomBorder");
                            if (prevBorder != null) {
                                if (prevBorder && !childBorders.top) {
                                    childWidget.addStyleName("topBorder");
                                    prevWidget.addStyleName("bottomBorder");
                                }
                            } else
                                top = childBorders.top;

                            prevBorder = childBorders.bottom;
                        } else {
                            top |= childBorders.top;
                            bottom |= childBorders.bottom;

                            childWidget.removeStyleName("leftBorder");
                            childWidget.removeStyleName("rightBorder");
                            if (prevBorder != null) {
                                if (prevBorder || childBorders.left) {
                                    childWidget.addStyleName("leftBorder");
                                    prevWidget.addStyleName("rightBorder");
                                }
                            } else
                                left = childBorders.left;

                            prevBorder = childBorders.right;
                        }
                        prevWidget = childWidget;
                    }
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
                            flexPanel.setStretchFlex(flexWidget, true);

                        mainAlignment = flexAlignment;
                    }
                } else // single already stretched element we're using the inner alignment
                    mainAlignment = flexChildAlignment;
            }

            horzAlignment = vertical ? oppositeAlignment : mainAlignment;
            vertAlignment = vertical ? mainAlignment : oppositeAlignment;
        }


        if (widget instanceof CaptionPanel || widget instanceof FlexTabbedPanel)
            top = left = right = bottom = hasBorders = true;

        return new DrawBorders(top, bottom, left, right, hasBorders, horzAlignment, vertAlignment);
    }
}
