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

    private GToolbarButton applyButton;

    public GFilterControlsView(GFiltersHandler handler) {
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
                return event -> handler.applyConditions();
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

    // the apply button shown in the manual apply mode, lit while something waits for it
    public void showApply(boolean shown, boolean lit) {
        applyButton.setVisible(shown);
        applyButton.setEnabled(lit);
        if(lit)
            GwtClientUtils.addClassName(applyButton, "active");
        else
            GwtClientUtils.removeClassName(applyButton, "active");
    }
}
