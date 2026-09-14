package lsfusion.gwt.client.form.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.FormsContainerPanel;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

// WINDOW ... FORMS without TABBED: the window shows one form, and nothing of its own around it - no tab strip, no
// close button, and none of the platform's toolbar, which belongs to the strip. Which form is shown is the
// application's to say, with SHOW and ACTIVATE.
// It still HOLDS more than one, because such a window can end up holding more than one: the arriving form asks the
// others to close, a close is a request, and a form with unsaved changes may stay. A form that stays is hidden, which
// is what a background tab already is, and ACTIVATE brings it back
public class SingleFormsView implements FormsView {

    // the marker FlexPanel's layout rules test for: the panel shows one of its children and hides the rest, so the
    // flex-line pass leaves it alone, and it gets the border and the padding of a container in its own right
    public static class Panel extends FlexPanel implements FormsContainerPanel {
        public Panel() {
            super(true);
        }
    }

    private final Panel panel = new Panel();
    private final FormsView.SelectionHandler selection;

    private int selected = -1;

    public SingleFormsView(FormsView.SelectionHandler selection) {
        this.selection = selection;

        GwtClientUtils.addClassName(panel, "forms-container");
    }

    @Override
    public Widget getView() {
        return panel;
    }

    @Override
    public void formAdded(FormDockable dockable, Integer index) {
        FlexPanel contentWidget = dockable.getContentWidget();

        int addIndex = index != null ? index : panel.getWidgetCount();
        panel.addFillShrink(contentWidget, addIndex);
        contentWidget.setVisible(false); // setCurrent shows it, right after - and it is the only caller that ever does

        // the form added BEFORE the shown one pushes it along, the same bookkeeping as the tab strip's insertTab. Only
        // a reopened form arrives at an index at all, and it arrives where it was
        if (addIndex <= selected)
            selected++;
    }

    @Override
    public void formRemoved(FormDockable dockable, int index) {
        // the same answer FlexTabBar.removeTab gives: the form being removed stops being the shown one, and an index
        // after it shifts down. FormsWindowController picks the next one afterwards
        if (index == selected) {
            selection.unselected(selected);
            selected = -1;
        } else if (index < selected)
            selected--;

        panel.remove(dockable.getContentWidget());
    }

    @Override
    public void setCurrent(int index) {
        if (index == selected)
            return;

        Widget shown = selected >= 0 ? panel.getWidget(selected) : null;

        selection.unselected(selected); // the blur while the form is still shown, which is where the tab strip does it

        if (shown != null)
            shown.setVisible(false);
        selected = index;
        if (index >= 0)
            panel.getWidget(index).setVisible(true);

        selection.selected(index); // and the focus once the form it goes to is showing

        FlexTabbedPanel.scheduleOnResize(panel); // being shown fires nothing in GWT, and this form was never measured
    }

    @Override
    public int getCurrent() {
        return selected;
    }

    @Override
    public void formsChanged() {
        // there is no strip, nothing around the form reads a caption or the blocked state, so there is nothing to refresh
    }
}
