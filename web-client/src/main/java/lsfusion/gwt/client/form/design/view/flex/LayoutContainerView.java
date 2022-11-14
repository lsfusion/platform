package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.CaptionPanel;
import lsfusion.gwt.client.base.view.CollapsiblePanel;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.view.MainFrame;

public abstract class LayoutContainerView extends GAbstractContainerView {
    protected final GFormController formController;

    protected LayoutContainerView(GContainer container, GFormController formController) {
        super(container);

        assert !container.tabbed;

        this.formController = formController;
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        for (int i = 0, size = children.size(); i < size; i++) {
            GComponent child = children.get(i);
            if (child instanceof GContainer) // optimization
                childrenViews.get(i).widget.setVisible(childrenVisible[i]);
        }

        super.updateLayout(requestIndex, childrenVisible);
    }

    protected FlexPanel wrapBorderImpl(GComponent child) {
        if (child instanceof GContainer) {
            GContainer childContainer = (GContainer) child;

            String caption = childContainer.caption;
            boolean border = childContainer.border;
            if(!MainFrame.useBootstrap)
                border = false;

            if (childContainer.collapsible)
                return new CollapsiblePanel(caption, border, collapsed -> formController.setContainerCollapsed(container, collapsed));
            else if (caption != null || border)
                return new CaptionPanel(caption, border);
        }
        return null;
    }

    public void updateCaption(GContainer container) {
        CaptionPanel captionPanel = null;
        FlexPanel childPanel = (FlexPanel) getChildView(container);

        // if we have caption it has to be either CaptionPanel, or it is wrapped into one more flexPanel (see addImpl)
        if(childPanel instanceof CaptionPanel) {
            captionPanel = (CaptionPanel) childPanel;
        } else {
            if(childPanel.getWidgetCount() > 0) {
                Widget childWidget = childPanel.getWidget(0);
                if(childWidget instanceof CaptionPanel)
                    captionPanel = (CaptionPanel) childWidget;
            }
        }

        String caption = container.caption;
        if(captionPanel != null)
            captionPanel.setCaption(caption);
        else // it is possible if hasNoCaption is true, so captionPanel is not created, however dynamic caption changes may come to the client
            assert caption == null;
    }
}
