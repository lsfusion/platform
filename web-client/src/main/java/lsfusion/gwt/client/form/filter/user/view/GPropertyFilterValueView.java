package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.List;

public class GPropertyFilterValueView extends GFilterValueView {
    public GPropertyFilterValueView(final GFilterValueListener listener, final GPropertyFilterValue propertyValue, GTableController logicsSupplier) {
        super(listener);

        final GFilterConditionListBox propertyView = new GFilterConditionListBox();

        propertyView.addStyleName("customFontPresenter");

        List<GPropertyDraw> properties = logicsSupplier.getPropertyDraws();
        for (GPropertyDraw property : properties) {
            propertyView.add(property, property.getNotEmptyCaption());
        }

        propertyValue.property = (GPropertyDraw) propertyView.getSelectedItem();

        propertyView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                propertyValue.property = (GPropertyDraw) propertyView.getSelectedItem();
                listener.valueChanged();
            }
        });

        add(propertyView);
    }
}
