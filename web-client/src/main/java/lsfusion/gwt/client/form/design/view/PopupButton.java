package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;

public class PopupButton extends FormButton {

    private GFormController formController;
    public PopupButton(GFormController formController) {
        super(Document.get().createAnchorElement());
        this.formController = formController;

        addStyleName("nav-link navbar-text");
    }

    public void setClickHandler(GContainer container, Widget widget) {
        addClickHandler(clickEvent -> {
            GwtClientUtils.showTippyPopup(null, getElement(), widget, () -> {
                formController.setContainerCollapsed(container, true);
            });
            formController.setContainerCollapsed(container, false);
        });
    }
}