package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;

public abstract class GFilterCompareSelector extends GFilterOptionSelector<GCompare> {
    private FocusPanel focusPanel;
    private CheckBox negationCB;
    private boolean negation;

    public GFilterCompareSelector(GPropertyFilter condition) {
        super(condition.property.baseType.getFilterCompares());
        this.negation = condition.negation;

        negationCB = new CheckBox("!");
        negationCB.addStyleName("userFilterNegationCheckBox");
        negationCB.setValue(negation);
        negationCB.addValueChangeHandler(event -> {
            GFilterCompareSelector.this.negation = negationCB.getValue();
            negationChanged(GFilterCompareSelector.this.negation);
            updateText();
        });

        FlexPanel popupContainer = new FlexPanel(true);
        popupContainer.add(negationCB);
        popupContainer.add(GwtClientUtils.createHorizontalSeparator());
        popupContainer.add(menuBar);
        
        focusPanel = new FocusPanel(popupContainer);
        focusPanel.addFocusHandler(event -> menuBar.focus());
    }

    @Override
    protected Widget getPopupContent() {
        return focusPanel;
    }

    public void set(GCompare[] values) {
        menuBar.clearItems();
        for (GCompare value : values) {
            addMenuItem(value, value.toString());
        }
    }

    @Override
    public void valueChanged(GCompare value) {
        updateText();
    }
    
    private void updateText() {
        setText((negation ? "!" : "") + currentValue);
    }

    public abstract void negationChanged(boolean value); 
}
