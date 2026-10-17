package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;

public class GFilterControlsView extends FlexPanel {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    // the platform's editors of the conditions, whose apply button this is - none where a view draws the conditions:
    // the button is never shown then, the mode never being manual
    private final GFilterEditors editors;

    private GToolbarButton applyButton;

    public GFilterControlsView(GFiltersHandler handler, GFilterEditors editors) {
        this.editors = editors;
       GwtClientUtils.addClassNames(this, "filter-controls", "btn-group", "btn-toolbar");
        
        if (handler.hasFiltersContainer()) {
            GToolbarButton addConditionButton = new GToolbarButton(StaticImage.ADD_FILTER, messages.formFilterAddCondition()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> handler.addCondition();
                }
            };
            GwtClientUtils.addClassName(addConditionButton, "filter-button");
            add(addConditionButton, GFlexAlignment.CENTER);
        }

        applyButton = new GToolbarButton(StaticImage.OK, messages.formFilterApply()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> editors.applyFilters();
            }
        };
        GwtClientUtils.addClassName(applyButton, "filter-button");
        add(applyButton, GFlexAlignment.CENTER);

        GToolbarButton resetConditionsButton = new GToolbarButton(StaticImage.RESET_FILTERS, messages.formFilterResetConditions()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    handler.resetConditions();
                };
            }
        };
        GwtClientUtils.addClassName(resetConditionsButton, "filter-button");
        add(resetConditionsButton, GFlexAlignment.CENTER);
    }

    public void setApplyEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
        if(enabled)
            GwtClientUtils.addClassName(applyButton, "active");
        else
            GwtClientUtils.removeClassName(applyButton, "active");
    }

    @Override
    public void setVisible(boolean nVisible) {
        super.setVisible(nVisible);
        
        if (nVisible) {
            applyButton.setVisible(editors != null && editors.isManualApplyMode());
        }
    }
}
