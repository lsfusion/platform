package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.filter.user.GObjectFilterValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public abstract class GObjectFilterValueView extends GFilterValueView {
    public final GFilterConditionListBox objectView;
    
    public GObjectFilterValueView(final GObjectFilterValue filterValue, GTableController logicsSupplier) {
        objectView = new GFilterConditionListBox() {
            @Override
            public void setFocused(NativeEvent event, boolean focused) {
                if (event instanceof Event) {
                    logicsSupplier.getForm().previewBlurEvent((Event) event);
                }
                GObjectFilterValueView.this.setFocused(focused);
            }
        };
        objectView.addStyleName("customFontPresenter");

        objectView.add(logicsSupplier.getObjects().toArray(new GObject[]{}));

        objectView.addChangeHandler(event -> filterValue.object = (GObject) objectView.getSelectedItem());

        filterValue.object = (GObject) objectView.getSelectedItem();

        add(objectView);
    }
}
