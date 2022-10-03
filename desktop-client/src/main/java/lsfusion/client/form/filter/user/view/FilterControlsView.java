package lsfusion.client.form.filter.user.view;

import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;

import static lsfusion.client.ClientResourceBundle.getString;

public class FilterControlsView extends FlexPanel {
    public static final String ADD_ICON_PATH = "filtadd.png";
    public static final String APPLY_ICON_PATH = "ok.png";
    public static final String RESET_ICON_PATH = "filtreset.png";
    
    private ToolbarGridButton applyButton;

    public FilterControlsView(FiltersHandler handler) {
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        
        if (handler.hasFiltersContainer()) {
            ToolbarGridButton addConditionButton = new ToolbarGridButton(ADD_ICON_PATH, getString("form.queries.filter.add.condition"));
            addConditionButton.addActionListener(ae -> handler.addCondition());
            add((Widget) addConditionButton, FlexAlignment.CENTER);
        }

        applyButton = new ToolbarGridButton(APPLY_ICON_PATH, getString("form.queries.filter.apply"));
        applyButton.addActionListener(ae -> handler.applyFilters(true));
        add((Widget) applyButton, FlexAlignment.CENTER);

        ToolbarGridButton resetConditionsButton = new ToolbarGridButton(RESET_ICON_PATH, getString("form.queries.filter.reset.conditions"));
        resetConditionsButton.addActionListener(e -> handler.resetConditions());
        add((Widget) resetConditionsButton, FlexAlignment.CENTER);
    }
    
    public void setApplyEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
    }
}
