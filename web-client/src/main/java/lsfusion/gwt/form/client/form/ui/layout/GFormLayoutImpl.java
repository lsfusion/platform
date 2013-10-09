package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.GPanelController;
import lsfusion.gwt.form.shared.view.GContainer;
import lsfusion.gwt.form.shared.view.GGrid;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;

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

    public static class RenderersPanel extends FlexPanel implements GPanelController.RenderersPanel {
        @Override
        public void add(PanelRenderer renderer) {
            add(renderer.getComponent(), GFlexAlignment.STRETCH, 1, "auto");
        }

        @Override
        public void remove(PanelRenderer renderer) {
            remove(renderer.getComponent());
        }

        @Override
        public Dimension getPreferredSize() {
            return GwtClientUtils.calculateStackPreferredSize(this.iterator(), false);
        }
    }
}
