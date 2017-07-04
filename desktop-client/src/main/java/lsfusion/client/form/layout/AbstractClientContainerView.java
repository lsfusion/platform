package lsfusion.client.form.layout;

import lsfusion.base.BaseUtils;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.FlexConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.client.SwingUtils.overrideSize;

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
        public ContainerViewPanel() {
            this(null);
        }

        public ContainerViewPanel(LayoutManager layout) {
            super(layout);
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
    }
}
