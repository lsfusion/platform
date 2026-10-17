package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButtonGroup;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;

// the platform's toolbar - TOOLBARSYSTEM where the platform draws it -, the platform's kind of the toolbar's
// controller, of a list's group (GPlatformGridToolbarController) or of a tree, one for all its groups
// (GPlatformTreeToolbarController), as a view's part is of either (GReactFormData.GridToolbarPart, TreeToolbarPart):
// its view, and the buttons on it - its table's, given to it as what draws the rows is (addRows), and the filter
// controller's, put where its table has it, whenever the filter input controller is made (addFilterToggle). Here is
// what both have: the table, the view, the filter button, and the export
public abstract class GPlatformToolbarController<R extends GTableController> implements GToolbarController<R> {
    protected static final ClientMessages messages = ClientMessages.Instance.get();

    protected final GFormController form;
    protected final GGridProperty table; // whose toolbar it is, and whose rows it exports
    // none where the design does not show the toolbar: the buttons are made all the same, and go nowhere
    private final GToolbarView view;

    protected GPlatformToolbarController(GFormController form, GGridProperty table) {
        this.form = form;
        this.table = table;

        if (!table.toolbar.visible) {
            view = null;
        } else {
            view = new GToolbarView();
            form.formLayout.addBaseComponent(table.toolbar, view, null);
        }
    }

    public void add(Widget tool) {
        if (view != null) {
            view.addComponent(tool);
        }
    }

    public void setVisible(boolean visible) {
        if (view != null)
            GwtClientUtils.setGridVisible(view, visible);
    }

    // ===== the filter button, which shows and hides its controls - pressed while they are shown -:
    // put where its table has it among the buttons already there, the filter input controller being made after what
    // draws the rows gives the toolbar its buttons (addRows)
    private GToolbarButton filterButton;

    public void addFilterToggle(GFilterInputController filter) {
        filterButton = new GToolbarButton(StaticImage.FILTER) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> filter.toggleControlsVisible();
            }
        };
        showFilterControls(filter.isControlsVisible());

        GToolbarButtonGroup filterButtonGroup = new GToolbarButtonGroup();
        filterButtonGroup.add(filterButton);
        if (view != null)
            view.insertComponent(filterButtonGroup, getFilterToggleIndex());
    }
    // ... before which of those buttons it goes: its table's answer
    protected abstract int getFilterToggleIndex();

    public void showFilterControls(boolean shown) {
        filterButton.setTitle(shown ? messages.formFilterHideControls() : messages.formFilterShowControls());
        filterButton.showBackground(shown);
    }

    // ===== the export of the rows - the server's report of them, or of the tree, or the table's own -: a button of its
    // own group, handed back to whoever enables it (GPivot)
    public GToolbarButton addExport(Runnable export) {
        GToolbarButton exportButton = new GToolbarButton(StaticImage.EXCELBW, messages.formGridExport()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> export.run();
            }
        };
        GToolbarButtonGroup exportButtonGroup = new GToolbarButtonGroup();
        exportButtonGroup.add(exportButton);
        add(exportButtonGroup);
        return exportButton;
    }
    // ... of the server's report of the rows - the group's, or the tree's -, where the design shows the export
    // (showPrintGroupXls): what needs no table of the platform's
    protected void addReportExport() {
        if (table.toolbar.showPrintGroupXls)
            addExport(() -> form.runTableReport(table));
    }
}
