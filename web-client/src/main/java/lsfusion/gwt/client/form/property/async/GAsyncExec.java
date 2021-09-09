package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

public abstract class GAsyncExec extends GAsyncEventExec {

    public abstract void exec(GAsyncFormController asyncFormController, FormsController formsController);

}