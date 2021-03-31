package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.List;

public class GPropertyFilterValueView extends GFilterValueView {
    public GPropertyFilterValueView(final GPropertyFilterValue propertyValue, GTableController logicsSupplier) {
        final GFilterConditionListBox propertyView = new GFilterConditionListBox();

        propertyView.addStyleName("customFontPresenter");

        List<GPropertyDraw> groupObjectProperties = logicsSupplier.getGroupObjectProperties();
        for (GPropertyDraw property : groupObjectProperties) {
            if(!(property.baseType instanceof GActionType)) {
                propertyView.add(property, property.getNotEmptyCaption());
            }
        }
        for (GPropertyDraw property : logicsSupplier.getPropertyDraws()) {
            if(!(property.baseType instanceof GActionType) && !groupObjectProperties.contains(property)) {
                propertyView.add(property, property.getFilterCaption());
            }
        }

        propertyValue.property = (GPropertyDraw) propertyView.getSelectedItem();

        propertyView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                propertyValue.property = (GPropertyDraw) propertyView.getSelectedItem();
            }
        });

        add(propertyView);
    }
}
