package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LayoutContainerView;

// a container whose own renderer decides WHERE each child view goes: an HTML template with [sID] slots
// (CustomContainerView) or a React tree with <LsfComponent sid/> placeholders (ReactContainerView).
//
// A child is a LOGICAL child of panel, so GWT attaches it (event listener, sinkEvents, onLoad), keeps it in the
// RequiresResize chain, and remove() finds its parent. Its ELEMENT meanwhile waits in a hidden park node, until the
// renderer places it. Placing and unplacing are therefore pure DOM moves: no onUnload/onLoad fires, so a placed grid
// keeps its state, and a renderer that re-places a child (a React re-mount) cannot stack duplicate widgets.
public abstract class ParkedContainerView extends LayoutContainerView {

    protected final ResizableComplexPanel panel = new ResizableComplexPanel();

    private boolean resizeScheduled;

    protected ParkedContainerView(GContainer container, GFormController formController) {
        super(container, formController);
    }

    @Override
    public Widget getView() {
        return panel;
    }

    // the park is the (invisible) attachContainer, so a parked element stays in the document — its widget is attached,
    // and lying about that would break events and sizing — but is hidden; and it is outside panel, so the renderer,
    // which owns panel's DOM and for React literally clears it on the first commit, never sees a parked element
    protected Element getParkElement() {
        return formController.getFormLayout().attachContainer.getElement();
    }

    @Override
    protected void addImpl(int index) {
        getChildView(index).attachTo(panel, getParkElement());
    }

    @Override
    protected void removeImpl(int index) {
        getChildView(index).remove(panel);
    }

    protected void parkChild(int index) {
        getChildView(index).appendTo(getParkElement());
    }

    // a child that just moved from the (display:none) park into a visible slot has never been measured, and a DOM move
    // fires nothing in GWT — so the resize is scheduled here, once the new position has been laid out
    protected void resizeChildren() {
        if (resizeScheduled) // one pass is enough however many children a single render placed
            return;
        resizeScheduled = true;
        Scheduler.get().scheduleDeferred(() -> {
            resizeScheduled = false;
            panel.onResize();
        });
    }
}
