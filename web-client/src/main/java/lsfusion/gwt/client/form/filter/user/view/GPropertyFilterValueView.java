package lsfusion.gwt.client.form.filter.user.view;

import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.List;

public abstract class GPropertyFilterValueView extends GFilterValueView {
    public final GFilterConditionListBox propertyView;
    
    public GPropertyFilterValueView(final GPropertyFilterValue propertyValue, GTableController logicsSupplier) {
        propertyView = new GFilterConditionListBox();

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

        propertyView.addFocusHandler(event -> setFocused(true));
        propertyView.addBlurHandler(event -> setFocused(false));
        propertyView.addChangeHandler(event -> propertyValue.property = (GPropertyDraw) propertyView.getSelectedItem());

        add(propertyView);
    }
}
