package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterControlsView;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;

// the platform's filter controls of a group, or of a tree for all its groups - FILTERCONTROLS where the platform draws
// it: its buttons, made once the filter input controller whose commands they give is, as they ask it what they offer,
// and put where the design places them - none where the platform makes no filter input controller -, its apply button
// shown in the manual apply mode, lit while something waits to be sent (a value prepared waits in either mode)
public final class GPlatformFilterControlsController implements GFilterControlsController, GUserFiltersController.View {
    private final GFormLayout layout; // where the design places them
    private final GFilterControls design;
    private GFilterControlsView view;
    private GUserFiltersController userFilters;

    public GPlatformFilterControlsController(GFormLayout layout, GFilterControls design) {
        this.layout = layout;
        this.design = design;
    }

    public void bind(GFiltersHandler handler, GUserFiltersController userFilters) {
        this.userFilters = userFilters;
        view = new GFilterControlsView(handler);
        layout.addBaseComponent(design, view, null);
        userFilters.addView(this);
        changed(null);
    }
    public boolean isShownByToolbar() {
        return true;
    }
    public boolean isApplyShown(boolean shownByToolbar) {
        return shownByToolbar;
    }
    public void changed(GUserFiltersController.Change change) {
        view.showApply(userFilters.isManual(), userFilters.isPending());
    }

    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }
    public void setTableVisible(boolean visible) {
        GwtClientUtils.setGridVisible(view, visible);
    }
}
