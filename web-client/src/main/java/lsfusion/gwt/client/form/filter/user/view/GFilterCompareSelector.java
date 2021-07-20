package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;

public abstract class GFilterCompareSelector extends GFilterOptionSelector<GCompare> {
    private FocusPanel focusPanel;
    
    private boolean negation;
    private CheckBox negationCB;
    
    private boolean allowNull;
    private CheckBox allowNullCB;

    public GFilterCompareSelector(GPropertyFilter condition) {
        super(condition.property.getFilterCompares());
        negation = condition.negation;

        negationCB = new CheckBox("!");
        negationCB.addStyleName("userFilterNegationCheckBox");
        negationCB.setValue(negation);
        negationCB.addValueChangeHandler(event -> {
            negation = negationCB.getValue();
            negationChanged(negation);
            updateText();
        });
        
        allowNullCB = new CheckBox(ClientMessages.Instance.get().formFilterConditionAllowNull());
        allowNullCB.addStyleName("userFilterNegationCheckBox");
        allowNullCB.setValue(allowNull);
        allowNullCB.addValueChangeHandler(event -> {
            allowNull = allowNullCB.getValue();
            allowNullChanged(allowNull);
        });

        FlexPanel popupContainer = new FlexPanel(true);
        popupContainer.add(menuBar, GFlexAlignment.STRETCH);
        popupContainer.add(GwtClientUtils.createHorizontalSeparator());
        popupContainer.add(negationCB);
        popupContainer.add(allowNullCB);
        
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
    protected MenuItem addMenuItem(GCompare value, String caption) {
        MenuItem menuItem = super.addMenuItem(value, caption);
        
        menuItem.setTitle(value.getTooltipText());
        
        return menuItem;
    }

    @Override
    public void valueChanged(GCompare value) {
        updateText();
    }

    @Override
    public void setText(String text) {
        super.setText((negation ? "!" : "") + text);
    }

    private void updateText() {
        setText(currentValue.toString());
        setTitle((negation ? "!" : "") + currentValue.getTooltipText());
    }

    public abstract void negationChanged(boolean value);
    public abstract void allowNullChanged(boolean value);
}
