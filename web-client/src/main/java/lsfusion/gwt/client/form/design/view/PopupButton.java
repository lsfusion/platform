package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;

public class PopupButton extends FormButton {

    private GFormController formController;
    public PopupButton(GFormController formController) {
        super(Document.get().createPushButtonElement());
        this.formController = formController;

        addStyleName("btn popup-panel-button");
    }

    public void setContent(GContainer container, Widget widget, ResizableComplexPanel attachContainer) {
        attachContainer.add(widget);
        addClickHandler(clickEvent -> {
            GwtClientUtils.showTippyPopup(PopupButton.this, getElement(), widget.getElement(), () -> {
                formController.setContainerCollapsed(container, true);
            });
            formController.setContainerCollapsed(container, false);
        });
    }
}