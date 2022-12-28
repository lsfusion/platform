package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;

import static lsfusion.gwt.client.view.StyleDefaults.COMPONENT_HEIGHT;

public class GFilterControlsView extends FlexPanel {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private GToolbarButton applyButton;

    public GFilterControlsView(GFiltersHandler handler) {
        addStyleName("filter-controls btn-group btn-toolbar");
        
        GSize buttonFlexBasis = GSize.CONST(COMPONENT_HEIGHT);
        
        if (handler.hasFiltersContainer()) {
            GToolbarButton addConditionButton = new GToolbarButton(StaticImage.ADD_FILTER, messages.formFilterAddCondition()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> handler.addCondition();
                }
            };
            addConditionButton.addStyleName("filter-button");
            add(addConditionButton, GFlexAlignment.CENTER, 0, false, buttonFlexBasis);
        }

        applyButton = new GToolbarButton(StaticImage.OK, messages.formFilterApply()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> handler.applyFilters();
            }
        };
        applyButton.addStyleName("filter-button");
        add(applyButton, GFlexAlignment.CENTER, 0, false, buttonFlexBasis);

        GToolbarButton resetConditionsButton = new GToolbarButton(StaticImage.RESET_FILTERS, messages.formFilterResetConditions()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    handler.resetConditions();
                };
            }
        };
        resetConditionsButton.addStyleName("filter-button");
        add(resetConditionsButton, GFlexAlignment.CENTER, 0, false, buttonFlexBasis);
    }

    public void setApplyEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
    }
}
