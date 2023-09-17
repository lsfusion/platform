package lsfusion.gwt.client.form.property.panel.view;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    private final SizedWidget sizedView;

    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, controller, property, columnKey, false);

        sizedView = value.getSizedWidget();

        if (property.drawAsync)
            form.setAsyncView(this);

        finalizeInit();
    }

    @Override
    public SizedWidget getSizedWidget() {
        return sizedView;
    }

    // hack, assert that render element is rendered with ActionCellRenderer
    @Override
    protected void setLabelText(String text) {
        BaseImage.updateText(value.getRenderElement(), text, property.panelCaptionVertical);
    }

    @Override
    protected void setLabelClasses(String classes) {
//        BaseImage.updateClasses(value.getRenderElement());
    }

    @Override
    protected void setCommentText(String text) {
    }

    @Override
    protected void setCommentClasses(String classes) {
    }

    // interface for refresh button
    public void setForceLoading(boolean set) {
        value.setForceLoading(set);
    }
}
