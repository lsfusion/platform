package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.client.form.filter.user.GObjectFilterValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GObjectFilterValueView extends GFilterValueView {

    public GObjectFilterValueView(final GObjectFilterValue filterValue, GTableController logicsSupplier) {
        final GFilterConditionListBox objectView = new GFilterConditionListBox();
        objectView.addStyleName("customFontPresenter");

        objectView.add(logicsSupplier.getObjects().toArray(new GObject[]{}));

        objectView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                filterValue.object = (GObject) objectView.getSelectedItem();
            }
        });

        filterValue.object = (GObject) objectView.getSelectedItem();

        add(objectView);
    }
}
