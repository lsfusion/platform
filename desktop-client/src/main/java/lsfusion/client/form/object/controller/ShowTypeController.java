package lsfusion.client.form.object.controller;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientShowType;
import lsfusion.client.form.object.view.ShowTypeView;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.interop.form.property.ClassViewType;

import java.io.IOException;

public class ShowTypeController {

    private final ClientShowType showType;
    private final TableController logicsSupplier;
    private final ClientFormController form;
    private final ShowTypeView view;

    public ShowTypeController(ClientGroupObject groupObject, final TableController logicsSupplier, final ClientFormController form) {
        this.showType = groupObject.showType;
        this.logicsSupplier = logicsSupplier;
        this.form = form;

        view = new ShowTypeView(this, showType, groupObject.banClassView);
    }

    public void addView(ClientFormLayout layout) {
        layout.add(showType, view);
    }

    public void changeClassViewButtonClicked(ClassViewType newClassView) {
        try {
            form.changeClassView(logicsSupplier.getGroupObject(), newClassView);
        } catch (IOException el) {
            throw new RuntimeException(ClientResourceBundle.getString("form.showtype.error.switching.type"), el);
        }
    }

    public void update(ClassViewType newClassView) {
        view.setClassView(newClassView);
    }
}