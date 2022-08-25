package lsfusion.client.form.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.design.view.widget.PanelWidget;
import lsfusion.client.form.design.view.widget.ScrollPaneWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexLayout;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlexPanel extends PanelWidget {

    protected static FlexPanelImpl impl = FlexPanelImpl.get();

    private final boolean vertical;

    public void checkMouseEvent(MouseEvent e) {
        checkResizeEvent(e, this);
    }
    public void checkResizeEvent(MouseEvent e, Component cursorElement) {
        ResizeHandler.checkResizeEvent(resizeHelper, cursorElement, e);
    }

    public FlexPanel() {
        this(false);
    }

    public FlexPanel(boolean vertical) {
        this(vertical, FlexAlignment.START);
    }

    public FlexPanel(boolean vertical, FlexAlignment alignment) {
        super(null);
        setLayout(new FlexLayout(this, vertical, alignment));
        this.vertical = vertical;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void add(Widget widget) {
        add(widget, FlexAlignment.START);
    }

    public void add(Widget widget, FlexAlignment alignment) {
        add(widget, getChildrenCount(), alignment);
    }

    private int getChildrenCount() {
        return getChildren().size();
    }

    public void add(Widget widget, int beforeIndex, FlexAlignment alignment) {
        add(widget, beforeIndex, alignment, 0, false, false, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addCentered(Widget child) {
        add(child, FlexAlignment.CENTER);
    }

    public void addFillFlex(Widget widget, Integer flexBasis) {
        addFill(widget, getChildrenCount(), flexBasis);
    }

    public void addFill(Widget widget) {
        addFill(widget, getChildrenCount());;
    }

    public void addFill(Widget widget, int beforeIndex) {
        addFill(widget, beforeIndex, null);
    }

    public void addFill(Widget widget, int beforeIndex, Integer flexBasis) {
        add(widget, beforeIndex, FlexAlignment.STRETCH, 1, false, false, flexBasis);
    }

    public void add(Widget widget, FlexAlignment alignment, double flex) {
        add(widget, alignment, flex, null);
    }

    public void add(Widget widget, FlexAlignment alignment, double flex, Integer flexBasis) {
        add(widget, getChildrenCount(), alignment, flex, false, false, flexBasis);
    }

    //main add method
    public void add(Widget widget, int beforeIndex, FlexAlignment alignment, double flex, boolean shrink, boolean alignShrink, Integer flexBasis) {
        add(widget.getComponent(), null, beforeIndex);

        LayoutData layoutData = impl.insertChild(getFlexLayout(), widget, beforeIndex, alignment, flex, shrink, alignShrink, flexBasis, vertical);
        widget.setLayoutData(layoutData);
    }

    private FlexLayout getFlexLayout() {
        return (FlexLayout) getLayout();
    }

    public ScrollPaneWidget wrapScrollPane;
    public boolean wrapFixedHorz;
    public boolean wrapFixedVert;
    public Dimension getDefaultPreferredSize() {
        return super.getPreferredSize();
    }

    // we need this to give inner STRETCH component upper size to have appropriate scroll
    @Override
    public Dimension getPreferredSize() {
        if(wrapScrollPane != null) {
            assert getFlexHeight() == null && getFlexWidth() == null;
            assert wrapScrollPane.wrapFlexPanel == this;
            return getFlexLayout().preferredFlexLayoutSize(this, wrapFixedHorz, wrapFixedVert);
        }
        return super.getPreferredSize();
    }

    public boolean remove(Widget widget) {
        remove(widget.getComponent());
        return true;
    }

    public static void setBaseSize(Widget element, boolean vertical, Integer size) {
        assert size == null || size >= 0;
        if(vertical)
            element.setFlexHeight(size);
        else
            element.setFlexWidth(size);
    }

    private ResizeHelper resizeHelper = new ResizeHelper() {
        @Override
        public int getChildCount() {
            return getChildrenCount();
        }

        @Override
        public Component getChildElement(int index) {
            return getChildren().get(index).getComponent();
        }

        @Override
        public Widget getChildWidget(int index) {
            return getChildren().get(index);
        }

        @Override
        public double resizeChild(int index, int delta) {
            return resizeWidget(index, delta);
        }
        @Override
        public boolean isChildResizable(int index) {
            if(!isWidgetResizable(index))
                return false;

            // optimization, if it is the last element, and there is a "resizable" parent, we consider this element to be not resizable (assuming that this "resizable" parent will be resized)
            if(index == getChildrenCount() - 1 && getParentSameFlexPanel(vertical) != null)
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

    // the resize algorithm assumes that there should be flex column to the left, to make
    public boolean isWidgetResizable(int widgetNumber) {
        List<Widget> children = getChildren();
        for (int i = widgetNumber; i >= 0; i--) {
            Widget child = children.get(i);
            LayoutData layoutData = child.getLayoutData();
            if (layoutData != null && layoutData.getBaseFlex() > 0 && child.isVisible())
                return true;
        }
        return false;
    }

    //ignore FilterView
    protected List<Widget> getChildren() {
        return BaseUtils.immutableCast(Arrays.stream(getComponents()).filter(component -> component instanceof Widget).collect(Collectors.toList()));
    }

    @Override
    public void add(Component comp, Object constraints) {
        throw new UnsupportedOperationException("main add method should be used instead");
//        super.add(comp, constraints);
    }

    // we need to guarantee somehow that resizing this parent container will lead to the same resizing of this container
    public Pair<FlexPanel, Integer> getParentSameFlexPanel(boolean vertical) {
//        if(1==1) return null;
        Component directChild = getComponent();
        Container parent = getParent();

        // unlike in web there can be PanelWidget.BorderLayout, and also ScrollPane + JViewport. We just ignore them
        while(!(parent instanceof ClientFormLayout) && !(parent instanceof FlexPanel)) {
            directChild = parent;
            parent = parent.getParent();
        }

        if(!(parent instanceof FlexPanel)) // it's some strange layouting, ignore it
            return null;

        Widget directChildWidget = (Widget)directChild;
        FlexPanel flexParent = (FlexPanel) parent;
        if(vertical != flexParent.vertical) {
            LayoutData layoutData = directChildWidget.getLayoutData();
            if(layoutData.alignment == FlexAlignment.STRETCH)
                return flexParent.getParentSameFlexPanel(vertical);
        } else {
            int index = flexParent.getChildren().indexOf(directChildWidget);
            if(flexParent.isWidgetResizable(index))
                return new Pair<>(flexParent, index);
        }
        return null;
    }

    public double resizeWidget(int widgetNumber, double delta) {
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
            LayoutData layoutData = widget.getLayoutData();
            if (layoutData.flexBasis != null)
                prefs[i] = layoutData.flexBasis;
            flexes[i] = layoutData.flex;
            // we don't need this since we can get actualSize other way (see comment below)
            if (layoutData.flexBasis == null || layoutData.baseFlexBasis == null) {
                layoutData.flex = 0.0;
                impl.setFlex(getFlexLayout(), widget, (Integer)null, vertical);
            }
            if (layoutData.baseFlexBasis != null)
                basePrefs[i] = layoutData.baseFlexBasis;
            baseFlexes[i] = layoutData.getBaseFlex();
            i++;
        }

        ClientFormLayout formLayout = SwingUtils.getClientFormLayout(this);
        // need validate to recalculate real sizes
        // in theory we can add getPreferredSize() Widget, but this way it is similar to the web client
        // but somewhy getPrefferedSize works worse than web way it seems that because branch getParentSameFlexPanel requires dropping flex and flexBasis
        formLayout.validate();

        // we'll do it in different cycles to minimize the quantity of layouting
        int margins = 0;
        i = 0;
        for(Widget widget : children) {
            LayoutData layoutData = widget.getLayoutData();

//            Dimension realDimension = widget.getPreferredSize();
//            int realSize = vertical ? realDimension.height : realDimension.width;
            int realSize = impl.getSize(widget.getComponent(), vertical); // calculating size
            if(layoutData.flexBasis == null || layoutData.baseFlexBasis == null) {
                if(layoutData.flexBasis == null)
                    prefs[i] = realSize;
                if(layoutData.baseFlexBasis == null)
                    basePrefs[i] = realSize;
            }
            //margins += impl.getFullSize(element, vertical) - realSize;
            i++;
        }

//        int body = ;
        // important to calculate viewWidth before setting new flexes
        int viewWidth = impl.getSize(this, vertical) - margins;
        double restDelta = SwingUtils.calculateNewFlexes(widgetNumber, delta, viewWidth, prefs, flexes, basePrefs, baseFlexes,  parentSameFlexPanel == null);

        if(parentSameFlexPanel != null && !SwingUtils.equals(restDelta, 0.0))
            restDelta = parentSameFlexPanel.first.resizeWidget(parentSameFlexPanel.second, restDelta);

        i = 0;
        for(Widget widget : children) {
            LayoutData layoutData = widget.getLayoutData();

            Integer newPref = (int) Math.round(prefs[i]);
            // if default (base) flex basis is auto and pref is equal to base flex basis, set flex basis to null (auto)
            if(newPref.equals(basePrefs[i]) && layoutData.baseFlexBasis == null)
                newPref = null;
            layoutData.flex = flexes[i];
            layoutData.flexBasis = newPref;
            impl.setFlex(getFlexLayout(), widget, layoutData, vertical);

            i++;
        }

        // need revalidate to repaint
        formLayout.revalidate();
        return restDelta;
    }

    private static void setFlexModifier(FlexLayout layout, boolean vertical, LayoutData layoutData, Widget child, FlexModifier modifier) {
        double prevFlex = layoutData.getFlex();
        layoutData.flexModifier = modifier;
        double newFlex = layoutData.getFlex();
        if (prevFlex != newFlex) 
            impl.setFlex(layout, child, layoutData, vertical);
    }

    public Widget getWidget(int i) {
        return (Widget) getComponent(i);
    }

    public int getWidgetIndex(Widget child) {
        return Arrays.asList(getComponents()).indexOf(child);
    }

    public static PanelParams updatePanels(Widget widget) {
        if (widget instanceof FlexPanel) {
            FlexPanel flexPanel = (FlexPanel) widget;
            boolean vertical = flexPanel.vertical;
            boolean forceVertCollapsed = flexPanel instanceof CollapsiblePanel && ((CollapsiblePanel) flexPanel).collapsed;
            boolean oppositeCollapsed = true;

            int flexCount = 0;
            boolean flexCollapsed = false;

            for (Widget child : flexPanel.getChildren()) {
                FlexPanel flexPanelChild = null;
                LayoutData flexLayoutData = child.getLayoutData();
                if (child instanceof FlexPanel) {
                    flexPanelChild = (FlexPanel) child;
                } else if (child instanceof ScrollPaneWidget) {
                    Component view = ((ScrollPaneWidget) child).getViewport().getView();
                    if (view instanceof FlexPanel) {
                        flexPanelChild = (FlexPanel) view;
                    }
                }
                
                if (flexPanelChild != null) {
                    PanelParams childParams = updatePanels(flexPanelChild);

                    boolean childOppositeCollapsed = vertical ? false : childParams.vertCollapsed;
                    oppositeCollapsed = oppositeCollapsed && childOppositeCollapsed;

                    boolean childMainCollapsed = vertical ? childParams.vertCollapsed : false;
                    if (childMainCollapsed)
                        flexCollapsed = true;

                    setFlexModifier(flexPanelChild.getFlexLayout(), vertical, flexLayoutData, child, childMainCollapsed ? FlexModifier.COLLAPSE : null);

                    boolean childMainFlex = flexLayoutData.isFlex();
                    if (childMainFlex)
                        flexCount++;
                } else if (((Component) child).isVisible()) {
                    return new PanelParams(false);
                }
            }

            // no flex elements left, but there are collapsed elements, we're collapsing this container
            boolean mainCollapsed = flexCount == 0 && flexCollapsed;

            boolean vertCollapsed = forceVertCollapsed || (vertical ? mainCollapsed : oppositeCollapsed);

            return new PanelParams(vertCollapsed);
        } else {
            return new PanelParams(false);
        }
    }

    public enum FlexModifier {
        COLLAPSE
    }

    private static class PanelParams {
        public final boolean vertCollapsed;

        public PanelParams(boolean vertCollapsed) {
            this.vertCollapsed = vertCollapsed;
        }
    }
}
