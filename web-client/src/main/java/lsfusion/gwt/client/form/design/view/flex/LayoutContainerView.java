package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.CaptionPanel;
import lsfusion.gwt.client.base.view.CollapsiblePanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;

public abstract class LayoutContainerView extends GAbstractContainerView {
    protected final GFormController formController;

    protected final boolean alignCaptions;

    protected LayoutContainerView(GContainer container, GFormController formController) {
        super(container);

        assert !container.tabbed;

        alignCaptions = container.isAlignCaptions();

        this.formController = formController;
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        for (int i = 0, size = children.size(); i < size; i++) {
            GComponent child = children.get(i);
            if (child instanceof GContainer) // optimization
                getChildView(i).setVisible(childrenVisible[i]);
        }

        super.updateLayout(requestIndex, childrenVisible);
    }

    protected Widget wrapBorderImpl(int index) {
        CaptionWidget childCaption;
        GComponent child = children.get(index);
        boolean border = child instanceof GContainer && ((GContainer) child).hasBorder();
        if(!(alignCaptions && child.isAlignCaption()) && ((childCaption = childrenCaptions.get(index)) != null || border)) {
            Widget childCaptionWidget = childCaption != null ? childCaption.widget.widget : null;

            boolean panelCaptionVertical = child.panelCaptionVertical;
            boolean panelCaptionLast = child.isPanelCaptionLast();
            GFlexAlignment panelCaptionAlignment = child.getPanelCaptionAlignment();
            if (child instanceof GContainer) {
                if (((GContainer) child).popup) {
                    return childCaptionWidget;
                } else if (((GContainer) child).collapsible) {
                    return new CollapsiblePanel(childCaptionWidget, border, collapsed -> formController.setContainerCollapsed((GContainer) child, collapsed), panelCaptionVertical, panelCaptionLast, panelCaptionAlignment);
                }
            }
            return new CaptionPanel(childCaptionWidget, border, panelCaptionVertical, panelCaptionLast, panelCaptionAlignment);
        }
        return null;
    }
}
