package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;

import static lsfusion.gwt.client.view.StyleDefaults.COMPONENT_HEIGHT;

public class GFilterControlsView extends FlexPanel {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String ADD_ICON_PATH = "filtadd.png";
    private static final String APPLY_ICON_PATH = "ok.png";
    private static final String RESET_ICON_PATH = "filtreset.png";

    public GFilterControlsView(GFiltersHandler handler) {
        GSize buttonFlexBasis = GSize.CONST(COMPONENT_HEIGHT);
        
        if (handler.hasFiltersContainer()) {
            GToolbarButton addConditionButton = new GToolbarButton(ADD_ICON_PATH, messages.formFilterAddCondition()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> handler.addCondition();
                }
            };
            addConditionButton.addStyleName("userFilterButton");
            add(addConditionButton, GFlexAlignment.CENTER, 0, false, buttonFlexBasis);
        }

        GToolbarButton applyButton = new GToolbarButton(APPLY_ICON_PATH, messages.formFilterApply()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> handler.applyFilters();
            }
        };
        applyButton.addStyleName("userFilterButton");
        add(applyButton, GFlexAlignment.CENTER, 0, false, buttonFlexBasis);

        GToolbarButton resetConditionsButton = new GToolbarButton(RESET_ICON_PATH, messages.formFilterResetConditions()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    handler.resetConditions();
                };
            }
        };
        resetConditionsButton.addStyleName("userFilterButton");
        add(resetConditionsButton, GFlexAlignment.CENTER, 0, false, buttonFlexBasis);
    }
}
