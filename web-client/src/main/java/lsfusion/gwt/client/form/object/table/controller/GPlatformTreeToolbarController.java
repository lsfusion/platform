package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButtonGroup;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeController;

// ... of a TREE - TOOLBARSYSTEM(TREE t), one for all its groups: the tree's buttons, whoever draws its rows, given to
// it as what draws them is (addRows), the filter button before them
public final class GPlatformTreeToolbarController extends GPlatformToolbarController<GTreeController, GTreeGroup> {

    public GPlatformTreeToolbarController(GFormController form, GTreeGroup tree) {
        super(form, tree);
    }

    // ===== what draws the tree's rows, given to it as it is made, whoever draws them: the two buttons that open the
    // tree through it - at the current node, and the tree, as what draws the rows opens it (GTreeController) -, and the
    // export, the server's report of the tree, where the design shows it (showPrintGroupXls, as a group's toolbar
    // does). They only open: the web client's tree closes nothing from its toolbar
    public void addRows(GTreeController rows) {
        GToolbarButtonGroup expandButtonGroup = new GToolbarButtonGroup();
        expandButtonGroup.add(new GToolbarButton(StaticImage.EXPANDTREECURRENT, messages.formTreeExpandCurrent()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> rows.expandCurrent();
            }
        });
        expandButtonGroup.add(new GToolbarButton(StaticImage.EXPANDTREE, messages.formTreeExpand()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> rows.expandTree();
            }
        });
        add(expandButtonGroup);

        addReportExport();
    }

    // ===== the filter button: before the tree's
    @Override
    protected int getFilterToggleIndex() {
        return 0;
    }
}
