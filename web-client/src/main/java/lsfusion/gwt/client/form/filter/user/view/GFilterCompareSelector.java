package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.ui.CheckBox;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.FormCheckBox;
import lsfusion.gwt.client.base.view.popup.PopupMenuItemValue;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;

import java.util.List;

public abstract class GFilterCompareSelector extends GFilterOptionSelector<GCompare> {
    public final String NOT_STRING = ClientMessages.Instance.get().formFilterCompareNot();

    private FlexPanel bottomPanel;
    
    private boolean negation;
    private CheckBox negationCB;
    
    private boolean allowNull;
    private CheckBox allowNullCB;

    public GFilterCompareSelector(GPropertyFilter condition, GFilterConditionView.UIHandler uiHandler, List<GCompare> values, List<String> popupCaptions, boolean allowNull) {
        super(uiHandler, values, popupCaptions);
        negation = condition.negation;
        this.allowNull = allowNull;

        negationCB = new FormCheckBox("! (" + NOT_STRING + ")");
        negationCB.setTitle(NOT_STRING);
        negationCB.addStyleName("filter-negation-check");
        negationCB.setValue(negation);
        negationCB.addValueChangeHandler(event -> {
            negation = negationCB.getValue();
            negationChanged(negation);
            updateText();
        });
        
        allowNullCB = new FormCheckBox(ClientMessages.Instance.get().formFilterConditionAllowNull());
        allowNullCB.addStyleName("filter-negation-check");
        allowNullCB.setValue(this.allowNull);
        allowNullCB.addValueChangeHandler(event -> {
            this.allowNull = allowNullCB.getValue();
            allowNullChanged(this.allowNull);
        });
        
        bottomPanel = new FlexPanel(true);
        bottomPanel.add(GwtClientUtils.createHorizontalSeparator());
        bottomPanel.add(negationCB);
        bottomPanel.add(allowNullCB);

        popup.addBottomPanelItem(bottomPanel);
    }

    public void set(GCompare[] values) {
        popup.clearItems();
        for (GCompare value : values) {
            addMenuItem(value, value.toString(), value.getFullString());
        }
    }

    @Override
    protected PopupMenuItemValue createItem(GCompare value, String caption, String popupCaption) {
        return new PopupMenuItemValue() {
            @Override
            public String getDisplayString() {
                return popupCaption;
            }

            @Override
            public String getReplacementString() {
                return caption;
            }

            @Override
            public String getTooltipString() {
                return value.getTooltipText();
            }
        };
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
        setTitle((negation ? NOT_STRING + " " : "") + currentValue.getTooltipText());
    }

    public abstract void negationChanged(boolean value);
    public abstract void allowNullChanged(boolean value);
}
