package lsfusion.client.form.layout;

import lsfusion.base.BaseUtils;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.Alignment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

public abstract class AbstractClientContainerView implements ClientContainerView {

    protected final ClientFormLayout formLayout;
    protected final ClientContainer container;

    protected final List<ClientComponent> children = new ArrayList<>();
    protected final List<JComponentPanel> childrenViews = new ArrayList<>();

    public AbstractClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        this.formLayout = formLayout;
        this.container = container;
    }

    @Override
    public void add(ClientComponent child, JComponentPanel view) {
        assert child != null && view != null && container.children.contains(child);

        int index = BaseUtils.relativePosition(child, container.children, children);

        children.add(index, child);
        childrenViews.add(index, view);

        addImpl(index, child, view);
    }

    public static void setSizes(JComponentPanel view, ClientComponent child) {
        view.setComponentMaximumSize(child.maximumSize);
        view.setComponentMinimumSize(child.minimumSize);
        view.setComponentPreferredSize(child.preferredSize);
    }
    public static void add(JPanel panel, JComponentPanel view, int index, Object constraints, ClientComponent child) {
        setSizes(view, child);
        panel.add(view, constraints, index);
    }

    @Override
    public void remove(ClientComponent child) {
        int index = children.indexOf(child);
        if (index == -1) {
            throw new IllegalStateException("Child wasn't added");
        }

        children.remove(index);
        JComponentPanel view = childrenViews.remove(index);

        removeImpl(index, child, view);
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

    public void updateLayout() {
        //do nothing by default
    }

    @Override
    public abstract JComponentPanel getView();
    protected abstract void addImpl(int index, ClientComponent child, JComponentPanel view);
    protected abstract void removeImpl(int index, ClientComponent child, JComponentPanel view);

    public class ContainerViewPanel extends JComponentPanel {
        public ContainerViewPanel(boolean vertical, Alignment alignment) {
            super(vertical, alignment);
            initBorder();
        }

        public ContainerViewPanel() {
            initBorder();
        }

        private void initBorder() {
            boolean isTabbed = container.isTabbed();
            boolean isInTabbed = container.container != null && container.container.isTabbed();
            if (!isTabbed && !isInTabbed) {
                String caption = container.getRawCaption();
                if (caption != null) {
                    setBorder(new TitledBorder(caption));
                }
            }

            container.installMargins(this);
        }

        @Override
        public boolean isValidateRoot() {
            return isTopContainerView();
        }

        @Override
        public void validate() {
            if (isTopContainerView()) {
                formLayout.preValidateMainContainer();
            }
            super.validate();
        }

        @Override
        protected void validateTree() {
            if (isTopContainerView()) {
                formLayout.preValidateMainContainer();
            }
            super.validateTree();
        }

        @Override
        public Dimension getMaxPreferredSize() {
            throw new UnsupportedOperationException(); // по идее должен обрабатываться по ветке ContainerView.getMaxPreferredSize
        }
    }

    public JComponentPanel getChildView(ClientComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenViews.get(index) : null;
    }

    // MAX PREFERRED SIZE
    // можно было бы попробовать общую схему LayoutManager'ов использовать, но проблема в том что каждый ContainerView может создавать внутренние компоненты с не определенными LayoutManager'ами, а как через них протянуть признак что нужен max, а не обычный preferredSize непонятно
    // поэтому пока используем логику из Web-Client'а (где LayoutManager'ом вообще CSS является)

    public static Dimension calculateMaxPreferredSize(JComponentPanel component) {
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
    private static Dimension getMaxPreferredSize(ClientComponent child, JComponentPanel childView) { // тут как и в GwtClientUtils.calculateMaxPreferredSize возможно нужна проверка на isVisible
        return overrideSize(child, calculateMaxPreferredSize(childView), true);
    }

    private static Dimension overrideSize(ClientComponent child, Dimension dimension, boolean max) {
        if(child.preferredSize == null)
            return dimension;
        
        int preferredWidth = child.preferredSize.width;
        if(preferredWidth == -1)
            preferredWidth = dimension.width;
        else if(max)        
            preferredWidth = BaseUtils.max(preferredWidth, dimension.width);
        
        int preferredHeight = child.preferredSize.height;
        if(preferredHeight == -1)
            preferredHeight = dimension.height;
        else if(max)
            preferredHeight = BaseUtils.max(preferredHeight, dimension.height);
        return new Dimension(preferredWidth, preferredHeight);
    }

    // не предполагает явное использование (так как не содержит проверки на явный preferredSize)
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

    private boolean needCaption() { // не top, не tabbed и есть caption
        return (container.container != null && !container.container.isTabbed()) && container.getRawCaption() != null;
    }

    public static Dimension enlargeDimension(Dimension dim, int extraWidth, int extraHeight) {
        dim.width += extraWidth;
        dim.height += extraHeight;
        return dim;
    }

    protected Dimension addCaptionDimensions(Dimension dimension) {
        if (needCaption()) {
            dimension.width += 10;
            dimension.height += 20;
        }
        return dimension;
    }
}
