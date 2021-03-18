package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.view.MainFrame;

public class GAsyncOpenForm extends GAsyncExec {
    public String caption;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncOpenForm() {
    }

    public GAsyncOpenForm(String caption, boolean modal) {
        this.caption = caption;
        this.modal = modal;
    }

    @Override
    public void exec(GFormController formController, GPropertyDraw property, Event event, ExecuteEditContext editContext) {
        formController.asyncOpenForm(this);
    }

    @Override
    public void exec(FormsController formsController) {
        formsController.asyncOpenForm(MainFrame.navigatorDispatchAsync.getNextRequestIndex(), this);
    }
}