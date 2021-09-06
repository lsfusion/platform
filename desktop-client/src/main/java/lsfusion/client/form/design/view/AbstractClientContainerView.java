package lsfusion.client.form.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

public abstract class AbstractClientContainerView implements ClientContainerView {

    protected final ClientContainer container;

    protected final boolean vertical;

    protected final List<ClientComponent> children = new ArrayList<>();
    protected final List<FlexPanel> childrenViews = new ArrayList<>();

    public AbstractClientContainerView(ClientContainer container) {
        this.container = container;

        vertical = container.isVertical();
    }

    @Override
    public void add(ClientComponent child, FlexPanel view) {
        assert child != null && view != null && container.children.contains(child);

        int index = BaseUtils.relativePosition(child, container.children, children);

        child.installMargins(view);

        boolean fixFlexBasis = child.isTab() && child.getFlex() > 0 && container.getFlexCount() > 1;

        children.add(index, child);
        childrenViews.add(index, wrapAndOverflowView(child, view, fixFlexBasis));

        addImpl(index, child, view);
    }

    private FlexPanel wrapAndOverflowView(ClientComponent child, FlexPanel view, boolean fixFlexBasis) {
        boolean isAutoSized = child.getSize(vertical) == null;
        boolean isOppositeAutoSized = child.getSize(!vertical) == null;

        if((!(isOppositeAutoSized && isAutoSized) || fixFlexBasis) && this instanceof LinearClientContainerView) { // child is tab, since basis is fixed, strictly speaking this all check is an optimization
            JScrollPane scroll = new JScrollPane() {
                @Override
                public void updateUI() {
                    super.updateUI();
                    setBorder(null); // is set on every color theme change in installDefaults()
                }
            };
            scroll.getVerticalScrollBar().setUnitIncrement(14);
            scroll.getHorizontalScrollBar().setUnitIncrement(14);
            ClientColorUtils.designComponent(scroll, container.design);
            JLayer<JScrollPane> scrollLayer = new JLayer<>(scroll, new MouseWheelScrollLayerUI());
            scroll.setViewportView(view);

            FlexPanel panel = new FlexPanel();
            // to forward a mouse wheel event in nested scroll pane to the parent scroll pane
            panel.add(scrollLayer, BorderLayout.CENTER);
            setSizes(panel, child);

            view = panel;
        }

        FlexPanel wrapPanel = wrapBorderImpl(child);
        if(wrapPanel != null) {
            // one of the main problem is that stretch (opposite direction) can give you flex-basis 0, and "appropriate" auto size, but with flex (main direction) you can not obtain this effect (actually we can by setting shrink !!!!), so we have to look at size
            // now since we have auto size we'll use the option without shrink
            wrapPanel.addFillFlex(view, isAutoSized ? null : 0); // we need zero basis, because overflow:auto is set for a view (not for this panel), and with auto size view will overflow this captionpanel
            // wrapPanel.addFillStretch could also be used in theory
            view = wrapPanel;
        }

        return view;
    }

    public static void setSizes(FlexPanel view, ClientComponent child) {
        //temp fix
        if (child.alignment == FlexAlignment.STRETCH) {
            if (child.container.isVertical()) {
                if (child.size.width == 0) 
                    child.size = new Dimension(-1, child.size.height);
            } else {
                if (child.size.height == 0) 
                    child.size = new Dimension(child.size.width, -1);
            }
        }
        view.setComponentSize(child.size);
    }
    public static void add(JPanel panel, FlexPanel view, int index, Object constraints, ClientComponent child) {
        setSizes(view, child);
        panel.add(view, constraints, index);
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

    @Override
    public Component getChildView(int index) {
        return childrenViews.get(index);
    }

    public void updateLayout(boolean[] childrenVisible) {
        //do nothing by default
    }

    @Override
    public abstract FlexPanel getView();
    protected abstract void addImpl(int index, ClientComponent child, FlexPanel view);
    protected abstract FlexPanel wrapBorderImpl(ClientComponent child);
    protected abstract void removeImpl(int index, ClientComponent child);

    public abstract void updateCaption(ClientContainer clientContainer);

    public FlexPanel getChildView(ClientComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenViews.get(index) : null;
    }

    // MAX PREFERRED SIZE
    // можно было бы попробовать общую схему LayoutManager'ов использовать, но проблема в том что каждый ContainerView может создавать внутренние компоненты с не определенными LayoutManager'ами, а как через них протянуть признак что нужен max, а не обычный size непонятно
    // поэтому пока используем логику из Web-Client'а (где LayoutManager'ом вообще CSS является)

    public static Dimension calculateMaxPreferredSize(FlexPanel component) {
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
    private static Dimension getMaxPreferredSize(ClientComponent child, FlexPanel childView) { // тут как и в GwtClientUtils.calculateMaxPreferredSize возможно нужна проверка на isVisible
        return overrideSize(child, calculateMaxPreferredSize(childView), true);
    }

    private static Dimension overrideSize(ClientComponent child, Dimension dimension, boolean max) {
        Dimension childSize = child.size;
        if(childSize == null)
            return dimension;
        
        int width = childSize.width;
        if(width == -1)
            width = dimension.width;
        else if(max)        
            width = BaseUtils.max(width, dimension.width);
        
        int preferredHeight = childSize.height;
        if(preferredHeight == -1)
            preferredHeight = dimension.height;
        else if(max)
            preferredHeight = BaseUtils.max(preferredHeight, dimension.height);
        return new Dimension(width, preferredHeight);
    }

    // не предполагает явное использование (так как не содержит проверки на явный size)
    public Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews) {
        boolean vertical = container.isVertical();
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

    public static void add(FlexPanel panel, FlexPanel widget, ClientComponent component, int beforeIndex) {
        boolean vertical = panel.isVertical();
        panel.add(widget, beforeIndex, component.getAlignment(), component.getFlex(), component.getSize(vertical));

        Integer crossSize = component.getSize(!vertical);
        boolean isStretch = component.isStretch();
        if(isStretch && crossSize != null && crossSize.equals(0)) // for opposite direction and stretch zero does not make any sense (it is zero by default)
            crossSize = null;
        FlexPanel.setBaseSize(widget, !vertical, crossSize, !isStretch);
    }
}
