package platform.gwt.form.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.client.FormDispatchAsync;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.view.logics.SelectObjectCallback;

public class SelectObjectDialog extends Window {
    public SelectObjectDialog(String objectCaption, FormDispatchAsync creationDispatcher, Action<GetFormResult> getFormAction, SelectObjectCallback selectObjectCallback) {
        setTitle("Выбор объекта: " + objectCaption);
        setShowMinimizeButton(false);
        setShowCloseButton(false);
        setShowModalMask(true);
        setIsModal(true);
        setCanDragResize(true);
        setCanDragReposition(true);
        setWidth(360);
        setHeight(115);
        setAutoSize(true);
        setAutoCenter(true);
        setOverflow(Overflow.VISIBLE);

        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                destroy();
            }
        });

        GFormController editorForm = new GFormController(creationDispatcher, getFormAction, true);

        Button btnClose = new Button("Закрыть");
        btnClose.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                destroy();
            }
        });

        addItem(editorForm);
        addItem(btnClose);
    }

    public static SelectObjectDialog showObjectDialog(String objectCaption, FormDispatchAsync creationDispatcher, Action<GetFormResult> getFormAction, SelectObjectCallback selectObjectCallback) {
        SelectObjectDialog wl = new SelectObjectDialog(objectCaption, creationDispatcher, getFormAction, selectObjectCallback);
        wl.show();
        return wl;
    }
}
