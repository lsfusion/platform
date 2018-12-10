package lsfusion.gwt.client.form.form.ui.filter;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.shared.form.view.GObject;
import lsfusion.gwt.shared.form.view.filter.GObjectFilterValue;
import lsfusion.gwt.shared.form.view.logics.GGroupObjectLogicsSupplier;

public class GObjectFilterValueView extends GFilterValueView {

    public GObjectFilterValueView(final GFilterValueListener listener, final GObjectFilterValue filterValue, GGroupObjectLogicsSupplier logicsSupplier) {
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
