package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.shared.form.design.GComponent;
import lsfusion.gwt.shared.form.design.GContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GFormLayout extends ResizableSimplePanel {
    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();

    private GFormController form;

    private GContainer mainContainer;

    private Map<GContainer, GAbstractContainerView> containerViews = new HashMap<>();

    private ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public GFormLayout(GFormController iform, GContainer imainContainer) {
        this.form = iform;

        mainContainer = imainContainer;

        createContainerViews(imainContainer);

        Widget mainContainerView = containerViews.get(imainContainer).getView();
        add(mainContainerView);

        layoutImpl.setupMainContainer(mainContainerView);
    }

    public GContainer getMainContainer() {
        return mainContainer;
    }

    private void createContainerViews(GContainer container) {
        GAbstractContainerView containerView = layoutImpl.createContainerView(form, container);

        containerViews.put(container, containerView);
        Widget view = containerView.getView();
        if (container.sID != null) {
            view.getElement().setAttribute("lsfusion-container", container.sID);
            view.getElement().setAttribute("lsfusion-container-type", container.type.name());
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
        return add(key, view, null);
    }

    public boolean add(GComponent key, Widget view, DefaultFocusReceiver focusReceiver) {
        if (key != null) {
            GAbstractContainerView containerView = containerViews.get(key.container);
            if (containerView != null && !containerView.hasChild(key)) {
                containerView.add(key, view);

                maybeAddDefaultFocusReceiver(key, focusReceiver);

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

                maybeRemoveDefaultFocusReceiver(key);

                return true;
            }
        }
        return false;
    }

    private void maybeAddDefaultFocusReceiver(GComponent key, DefaultFocusReceiver focusReceiver) {
        if (key.defaultComponent && focusReceiver != null) {
            defaultComponents.add(key);
            defaultFocusReceivers.add(focusReceiver);
        }
    }

    private void maybeRemoveDefaultFocusReceiver(GComponent key) {
        int index = defaultComponents.indexOf(key);
        if (index != -1) {
            defaultComponents.remove(index);
            defaultFocusReceivers.remove(index);
        }
    }

    public boolean focusDefaultWidget() {
        for (DefaultFocusReceiver dc : defaultFocusReceivers) {
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

    @Override
    public Dimension getMaxPreferredSize() {
        Dimension result = GAbstractContainerView.getMaxPreferredSize(mainContainer, containerViews, false); // в BOX container'е берем явный size (предполагая что он используется не как базовый размер с flex > 0, а конечный)
        setDebugDimensionsAttributes(containerViews.get(mainContainer).getView(), result);
        return result;
    }

    public static void setDebugDimensionsAttributes(Widget w, Dimension result) {
        w.getElement().setAttribute("lsfusion-size", "(" + result.width + ", " + result.height + ")");
    }
}