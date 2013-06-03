package lsfusion.gwt.form.client.form.ui.filter;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.form.shared.view.GObject;
import lsfusion.gwt.form.shared.view.filter.GObjectFilterValue;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

public class GObjectFilterValueView extends GFilterValueView {

    public GObjectFilterValueView(final GFilterValueListener listener, final GObjectFilterValue filterValue, GGroupObjectLogicsSupplier logicsSupplier) {
        super(listener);
        final GFilterConditionListBox objectView = new GFilterConditionListBox();
        objectView.addStyleName("customFontPresenter");

        objectView.add(logicsSupplier.getObjects().toArray(new GObject[]{}));

        objectView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                filterValue.object = (GObject) objectView.getSelectedValue();
                listener.valueChanged();
            }
        });

        filterValue.object = (GObject) objectView.getSelectedValue();

        add(objectView);
    }
}
