package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.DefaultFocusReceiver;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.gwt.base.client.GwtClientUtils.setupFillParent;

public class GFormLayout extends FlexPanel {
    private GFormController form;
    private GContainer mainContainer;

    private Map<GContainer, GAbstractContainerView> containerViews = new HashMap<GContainer, GAbstractContainerView>();
    private Widget mainContainerView;

    private ArrayList<DefaultFocusReceiver> defaultComponents = new ArrayList<DefaultFocusReceiver>();

    public GFormLayout(GFormController iform, GContainer imainContainer) {
        super(true);

        this.form = iform;

        mainContainer = imainContainer;

        createContainerViews(imainContainer);

        mainContainerView = containerViews.get(imainContainer).getView();

//        setSize("100%", "100%");

        setupFillParent(getElement());
        getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        add(mainContainerView, GFlexAlignment.STRETCH, 1);
    }

    private void createContainerViews(GContainer container) {
        GAbstractContainerView containerView;
        if (container.isLinear()) {
            containerView = new GLinearContainerView(container);
        } else if (container.isSplit()) {
            containerView = new GSplitContainerView(container);
        } else if (container.isTabbed()) {
            containerView = new GTabbedContainerView(form, container);
        } else if (container.isColumns()) {
            //todo:
            containerView = new GColumnsContainerView(container);
//            containerView = new GLinearContainerView(container);
        } else {
            throw new IllegalStateException("Incorrect container type");
        }

        containerViews.put(container, containerView);
        Widget view = containerView.getView();
        if (container.sID != null) {
            view.getElement().setAttribute("lsfusion-sid", container.sID);
        }

        if (container.container != null) {
            add(container, view);
        }

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                createContainerViews((GContainer) child);
            }
        }
    }

    public boolean add(GComponent key, Widget view) {
        if (key != null) {
            GAbstractContainerView containerView = containerViews.get(key.container);
            if (containerView != null && !containerView.hasChild(key)) {
                containerView.add(key, view);

                maybeAddDefaultComponent(key, view);

                return true;
            }
        }
        return false;
    }

    public boolean remove(GComponent key, Widget view) {
        if (key != null) {
            GAbstractContainerView containerView = containerViews.get(key.container);
            if (containerView != null && containerView.hasChild(key)) {
                containerView.remove(key);

                maybeRemoveDefaultComponent(key, view);

                return true;
            }
        }
        return false;
    }

    private void maybeAddDefaultComponent(GComponent key, Widget component) {
        if (key.defaultComponent && (component instanceof DefaultFocusReceiver)) {
            defaultComponents.add((DefaultFocusReceiver) component);
        }
    }

    private void maybeRemoveDefaultComponent(GComponent key, Widget component) {
        if (component instanceof DefaultFocusReceiver) {
            defaultComponents.remove(component);
        }
    }

    public boolean focusDefaultWidget() {
        for (DefaultFocusReceiver dc : defaultComponents) {
            if (dc.focus()) {
                return true;
            }
        }
        return false;
    }

    public GAbstractContainerView getFormContainer(GContainer container) {
        return containerViews.get(container);
    }

    public void hideEmptyContainerViews() {
        autoShowHideContainers(mainContainer);
    }

    private void autoShowHideContainers(GContainer container) {
        GAbstractContainerView containerView = containerViews.get(container);
        int childCnt = containerView.getChildrenCount();
        boolean hasVisible = false;
        for (int i = 0; i < childCnt; ++i) {
            GComponent child = containerView.getChild(i);
            Widget childView = containerView.getChildView(i);
            if (child instanceof GContainer) {
                autoShowHideContainers((GContainer) child);
            }

            if (childView.isVisible()) {
                hasVisible = true;
            }
        }
        containerView.getView().setVisible(hasVisible);
        containerView.updateLayout();
    }

    public void totalResize() {
        //todo: ??
    }

    public int getMainContainerWidth() {
        if (mainContainerView != null) {
            return mainContainerView.getOffsetWidth();
        }
        return -1;
    }

    public int getMainContainerHeight() {
        if (mainContainerView != null) {
            return mainContainerView.getOffsetHeight();
        }
        return -1;
    }
}
