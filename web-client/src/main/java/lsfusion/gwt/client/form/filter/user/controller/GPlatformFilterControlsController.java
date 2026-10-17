package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.view.GFilterControlsView;
import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;

// the platform's filter controls of a group, or of a tree for all its groups - FILTERCONTROLS where the platform draws
// it: its buttons, made once the filter input controller whose commands they give is, as they ask it what they offer,
// and put where the design places them - none where the platform makes no filter input controller
public final class GPlatformFilterControlsController implements GFilterControlsController {
    private final GFormController form;
    private final GFilterControls design;
    private GFilterControlsView view;

    public GPlatformFilterControlsController(GFormController form, GFilterControls design) {
        this.form = form;
        this.design = design;
    }

    public void bind(GFiltersHandler handler, GFilterEditors editors) {
        view = new GFilterControlsView(handler, editors);
        form.formLayout.addBaseComponent(design, view, null);
    }
    public boolean isShownByToolbar() {
        return true;
    }

    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }
    public void setApplyEnabled(boolean enabled) {
        view.setApplyEnabled(enabled);
    }
    public void setTableVisible(boolean visible) {
        GwtClientUtils.setGridVisible(view, visible);
    }
}
