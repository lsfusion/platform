package lsfusion.gwt.client.form.filter.user.view;

import lsfusion.gwt.client.form.filter.user.GObjectFilterValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public abstract class GObjectFilterValueView extends GFilterValueView {

    public GObjectFilterValueView(final GObjectFilterValue filterValue, GTableController logicsSupplier) {
        final GFilterConditionListBox objectView = new GFilterConditionListBox();
        objectView.addStyleName("customFontPresenter");

        objectView.add(logicsSupplier.getObjects().toArray(new GObject[]{}));

        objectView.addFocusHandler(event -> setFocused(true));
        objectView.addBlurHandler(event -> setFocused(false));
        objectView.addChangeHandler(event -> filterValue.object = (GObject) objectView.getSelectedItem());

        filterValue.object = (GObject) objectView.getSelectedItem();

        add(objectView);
    }
}
