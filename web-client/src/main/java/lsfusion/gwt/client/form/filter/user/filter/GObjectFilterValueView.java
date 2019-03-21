package lsfusion.gwt.client.form.filter.user.filter;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.shared.form.object.GObject;
import lsfusion.gwt.shared.form.filter.user.GObjectFilterValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GObjectFilterValueView extends GFilterValueView {

    public GObjectFilterValueView(final GFilterValueListener listener, final GObjectFilterValue filterValue, GTableController logicsSupplier) {
        super(listener);
        final GFilterConditionListBox objectView = new GFilterConditionListBox();
        objectView.addStyleName("customFontPresenter");

        objectView.add(logicsSupplier.getObjects().toArray(new GObject[]{}));

        objectView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                filterValue.object = (GObject) objectView.getSelectedItem();
                listener.valueChanged();
            }
        });

        filterValue.object = (GObject) objectView.getSelectedItem();

        add(objectView);
    }
}
