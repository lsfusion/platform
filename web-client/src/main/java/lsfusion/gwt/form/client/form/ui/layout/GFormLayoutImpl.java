package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.GPanelController;
import lsfusion.gwt.form.shared.view.GContainer;
import lsfusion.gwt.form.shared.view.GGrid;
import lsfusion.gwt.form.shared.view.panel.ActionPanelRenderer;
import lsfusion.gwt.form.shared.view.panel.DataPanelRenderer;

public abstract class GFormLayoutImpl {
    private static GFormLayoutImpl impl;

    public static GFormLayoutImpl get() {
        if (impl == null) {
            impl = GWT.create(GFormLayoutImpl.class);
        }
        return impl;
    }

    public abstract GAbstractContainerView createContainerView(GFormController form, GContainer container);

    public abstract void setupMainContainer(Widget mainContainerWidget);

    public abstract Panel createGridView(GGrid grid, Panel panel);

    public abstract GPanelController.RenderersPanel createRenderersPanel();

    public abstract void setupActionPanelRenderer(GPanelController.GPropertyController controller, ActionPanelRenderer actionRenderer);

    public abstract void setupDataPanelRenderer(GPanelController.GPropertyController controller, DataPanelRenderer dataRenderer);
}
