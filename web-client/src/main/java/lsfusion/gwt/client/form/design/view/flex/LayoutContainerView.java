package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.view.CaptionPanel;
import lsfusion.gwt.client.base.view.CollapsiblePanel;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.view.MainFrame;

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
                childrenViews.get(i).widget.setVisible(childrenVisible[i]);
        }

        super.updateLayout(requestIndex, childrenVisible);
    }

    protected FlexPanel wrapBorderImpl(int index) {
        CaptionWidget childCaption;
        GComponent child = children.get(index);
        if(!(alignCaptions && child.isAlignCaption()) && (childCaption = childrenCaptions.get(index)) != null) {
            Widget childCaptionWidget = childCaption.widget.widget;

            boolean border = child instanceof GContainer && ((GContainer) child).hasBorder();
            if (child instanceof GContainer && ((GContainer) child).collapsible)
                return new CollapsiblePanel(childCaptionWidget, border, collapsed -> formController.setContainerCollapsed(container, collapsed));
            else
                return new CaptionPanel(childCaptionWidget, border);
        }
        return null;
    }
}
