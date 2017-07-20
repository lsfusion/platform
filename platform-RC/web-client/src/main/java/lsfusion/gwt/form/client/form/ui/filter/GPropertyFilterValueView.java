package lsfusion.gwt.form.client.form.ui.filter;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.filter.GPropertyFilterValue;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.util.List;

public class GPropertyFilterValueView extends GFilterValueView {
    public GPropertyFilterValueView(final GFilterValueListener listener, final GPropertyFilterValue propertyValue, GGroupObjectLogicsSupplier logicsSupplier) {
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
