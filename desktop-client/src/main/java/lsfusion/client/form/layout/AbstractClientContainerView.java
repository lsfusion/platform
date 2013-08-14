package lsfusion.client.form.layout;

import lsfusion.base.BaseUtils;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.client.SwingUtils.overrideSize;

public abstract class AbstractClientContainerView implements ClientContainerView {

    protected final ClientFormLayout formLayout;
    protected final ClientContainer container;

    protected final List<ClientComponent> children = new ArrayList<ClientComponent>();
    protected final List<Component> childrenViews = new ArrayList<Component>();

    public AbstractClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        this.formLayout = formLayout;
        this.container = container;
    }

    @Override
    public void add(ClientComponent child, Component view) {
        assert child != null && view != null && container.children.contains(child);

        int index = BaseUtils.relativePosition(child, container.children, children);

        children.add(index, child);
        childrenViews.add(index, view);

        addImpl(index, child, view);
    }

    @Override
    public void remove(ClientComponent child) {
        int index = children.indexOf(child);
        if (index == -1) {
            throw new IllegalStateException("Child wasn't added");
        }

        children.remove(index);
        Component view = childrenViews.remove(index);

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
    public abstract JComponent getView();
    protected abstract void addImpl(int index, ClientComponent child, Component view);
    protected abstract void removeImpl(int index, ClientComponent child, Component view);

    public class ContainerViewPanel extends JPanel {
        public ContainerViewPanel() {
            this(null);
        }

        public ContainerViewPanel(LayoutManager layout) {
            this(layout, true);
        }

        public ContainerViewPanel(LayoutManager layout, boolean initBorder) {
            super(layout);
            if (initBorder) {
                initBorder();
            }
        }

        private void initBorder() {
            if (isTopContainerView() || !(container.container.isTabbed())) {
                String caption = container.getRawCaption();
                if (caption != null) {
                    super.setBorder(new TitledBorder(caption));
                }
            }
        }

        @Override
        public void setBorder(Border border) {
        }

        @Override
        public Dimension getMinimumSize() {
            return overrideSize(super.getMinimumSize(), container.minimumSize);
        }

        @Override
        public Dimension getMaximumSize() {
            return overrideSize(super.getMaximumSize(), container.maximumSize);
        }

        @Override
        public Dimension getPreferredSize() {
            return overrideSize(super.getPreferredSize(), container.preferredSize);
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
    }
}
