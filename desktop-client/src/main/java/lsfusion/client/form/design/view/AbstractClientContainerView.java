package lsfusion.client.form.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.flex.LinearClientContainerView;
import lsfusion.client.form.design.view.widget.ScrollPaneWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

public abstract class AbstractClientContainerView implements ClientContainerView {

    protected final ClientContainer container;

    protected final boolean vertical;

    protected final List<ClientComponent> children = new ArrayList<>();
    protected final List<Widget> childrenViews = new ArrayList<>();

    public AbstractClientContainerView(ClientContainer container) {
        this.container = container;

        vertical = !container.horizontal;
    }

    @Override
    public void add(ClientComponent child, Widget view) {
        assert child != null && view != null && container.children.contains(child);

        int index = BaseUtils.relativePosition(child, container.children, children);

        child.installMargins(view.getComponent());

        boolean fixFlexBasis = false; // we don't need this for now, since tabbed pane in desktop uses max preferredSize
        // child.isTab() && child.getFlex() > 0 && container.getFlexCount() > 1;

        children.add(index, child);
        childrenViews.add(index, wrapAndOverflowView(child, view, fixFlexBasis));

        addImpl(index, child, view);
    }

    private Widget wrapAndOverflowView(ClientComponent child, Widget view, boolean fixFlexBasis) {
        boolean isAutoSized = child.getSize(vertical) == null;
        boolean isOppositeAutoSized = child.getSize(!vertical) == null;

        // somewhy it doesn't work properly for tabbed client container, but it's not that important for now
        if((!(isOppositeAutoSized && isAutoSized) || child.shrink || child.alignShrink || fixFlexBasis)) // child is tab, since basis is fixed, strictly speaking this all check is an optimization
            view = wrapOverflowAuto(view, vertical ? !isOppositeAutoSized : !isAutoSized, vertical ? !isAutoSized : !isOppositeAutoSized);

        FlexPanel wrapPanel = wrapBorderImpl(child);
        if(wrapPanel != null) {
            wrapPanel.setDebugContainer(wrapDebugContainer("BORDER", view));
            // one of the main problem is that stretch (opposite direction) can give you flex-basis 0, and "appropriate" auto size, but with flex (main direction) you can not obtain this effect (actually we can by setting shrink !!!!), so we have to look at size
            // now since we have auto size we'll use the option without shrink
            wrapPanel.addFillFlex(view, isAutoSized ? null : 0); // we need zero basis, because overflow:auto is set for a view (not for this panel), and with auto size view will overflow this captionpanel
            // wrapPanel.addFillStretch could also be used in theory
            view = wrapPanel;
        }

        // we don't need this in desktop client
//        if(isOppositeAutoSized && child.isStretch()) {
//            wrapPanel = new FlexPanel(!vertical); // this container will have upper container size, but since the default overflow is visible, inner component with overflow:auto
//            wrapPanel.setStyleName("oppositeStretchAutoSizePanel"); // just to identify this div in dom
//            wrapPanel.addFillFlex(view, null);
//            view = wrapPanel;
//        }
        return view;
    }

    public static Object wrapDebugContainer(String prefix, Widget widget) {
        return new Object() {
            @Override
            public String toString() {
                return prefix + " [ " + widget.getDebugContainer() + " ] ";
            }
        };
    }
    public static Widget wrapOverflowAuto(Widget view, boolean fixedHorz, boolean fixedVert) {
//        assert view instanceof Scrollable;
        ScrollPaneWidget scroll = new ScrollPaneWidget(view.getComponent()) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(null); // is set on every color theme change in installDefaults()
            }
        };
        // need this to force view use getFlexPreferredSize for STRETCH not auto sized direction instead of getPreferredSize (web browser does that)
        if(view instanceof FlexPanel) {
            scroll.wrapFlexPanel = (FlexPanel) view;
            FlexPanel flexView = (FlexPanel) view;
            flexView.wrapScrollPane = scroll;
            flexView.wrapFixedHorz = fixedHorz;
            flexView.wrapFixedVert = fixedVert;
        }

        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getHorizontalScrollBar().setUnitIncrement(14);
        // to forward a mouse wheel event in nested scroll pane to the parent scroll pane
        scroll.setDebugContainer(wrapDebugContainer("SCROLL", view));

        return scroll;
    }

    @Override
    public void remove(ClientComponent child) {
        int index = children.indexOf(child);
        if (index == -1) {
            throw new IllegalStateException("Child wasn't added");
        }

        removeImpl(index, child);

        children.remove(index);
        childrenViews.remove(index);
    }

    protected boolean isTopContainerView() {
        return container.container == null;
    }

    @Override
    public boolean hasChild(ClientComponent child) {
        return children.contains(child);
    }

    @Override
    public int getChildrenCount() {
        return children.size();
    }

    @Override
    public ClientComponent getChild(int index) {
        return children.get(index);
    }

    public void updateLayout(boolean[] childrenVisible) {
        //do nothing by default
    }

    @Override
    public abstract Widget getView();
    protected abstract void addImpl(int index, ClientComponent child, Widget view);
    protected abstract FlexPanel wrapBorderImpl(ClientComponent child);
    protected abstract void removeImpl(int index, ClientComponent child);

    public abstract void updateCaption(ClientContainer clientContainer);

    public Widget getChildView(ClientComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenViews.get(index) : null;
    }

    // MAX PREFERRED SIZE
    // можно было бы попробовать общую схему LayoutManager'ов использовать, но проблема в том что каждый ContainerView может создавать внутренние компоненты с не определенными LayoutManager'ами, а как через них протянуть признак что нужен max, а не обычный size непонятно
    // поэтому пока используем логику из Web-Client'а (где LayoutManager'ом вообще CSS является)

    public static Dimension calculateMaxPreferredSize(Widget component) {
        return component.getMaxPreferredSize();
    }

    protected Dimension getChildMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews, int index) {
        return getChildMaxPreferredSize(containerViews, getChild(index));
    }

    protected Dimension getChildMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews, ClientComponent child) {
        Dimension dimensions = child instanceof ClientContainer
                ? getMaxPreferredSize((ClientContainer) child, containerViews, true)
                : getMaxPreferredSize(child, getChildView(child));
        Dimension result = enlargeDimension(dimensions, child.getHorizontalMargin(), child.getVerticalMargin());
        return result;
    }
    
    public static Dimension getMaxPreferredSize(ClientContainer child, Map<ClientContainer, ClientContainerView> containerViews, boolean max) {
        return overrideSize(child, containerViews.get(child).getMaxPreferredSize(containerViews), max);
    }
    private static Dimension getMaxPreferredSize(ClientComponent child, Widget childView) { // тут как и в GwtClientUtils.calculateMaxPreferredSize возможно нужна проверка на isVisible
        return overrideSize(child, calculateMaxPreferredSize(childView), true);
    }

    private static Dimension overrideSize(ClientComponent child, Dimension dimension, boolean max) {
        Integer width = child.getSize(false);
        if(width == null)
            width = dimension.width;
        else if(max)        
            width = BaseUtils.max(width, dimension.width);
        
        Integer preferredHeight = child.getSize(true);
        if(preferredHeight == null)
            preferredHeight = dimension.height;
        else if(max)
            preferredHeight = BaseUtils.max(preferredHeight, dimension.height);
        return new Dimension(width, preferredHeight);
    }

    // не предполагает явное использование (так как не содержит проверки на явный size)
    public Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews) {
        boolean vertical = !container.horizontal;
        int width = 0;
        int height = 0;
        int chCnt = children.size();
        for (int i = 0; i < chCnt; ++i) {
            if (childrenViews.get(i).isVisible()) {
                Dimension childSize = getChildMaxPreferredSize(containerViews, i);
                if (vertical) {
                    width = max(width, childSize.width);
                    height += childSize.height;
                } else {
                    width += childSize.width;
                    height = max(height, childSize.height);
                }
            }
        }

        return addCaptionDimensions(new Dimension(width, height));
    }

    private boolean hasCaption() { // не top, не tabbed и есть caption
        return !container.isTab() && !container.main && container.caption != null;
    }

    public static Dimension enlargeDimension(Dimension dim, int extraWidth, int extraHeight) {
        dim.width += extraWidth;
        dim.height += extraHeight;
        return dim;
    }

    protected Dimension addCaptionDimensions(Dimension dimension) {
        if (hasCaption()) {
            dimension.width += 10;
            dimension.height += 20;
        }
        return dimension;
    }

    public static void add(FlexPanel panel, Widget widget, ClientComponent component, int beforeIndex) {
        boolean vertical = panel.isVertical();
        panel.add(widget, beforeIndex, component.getAlignment(), component.getFlex(), component.isShrink(), component.isAlignShrink(), component.getSize(vertical));

        Integer crossSize = component.getSize(!vertical);
//        boolean isStretch = component.isStretch();
//        if(isStretch && crossSize != null && crossSize.equals(0)) // for opposite direction and stretch zero does not make any sense (it is zero by default)
//            crossSize = null;
        FlexPanel.setBaseSize(widget, !vertical, crossSize); // !isStretch
    }
}
