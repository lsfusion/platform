package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.Park;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.EditMode;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.FormsWindowController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.List;

// WINDOW forms CUSTOM 'FormsView': a React component draws the forms window - its own tabs, a breadcrumb, or nothing but
// the current form. It never draws a FORM: it renders a host per form it wants shown, and the platform moves that
// form's own view in, the same crossing <Platform name/> makes for a design child and a navigator element.
//
// A form the component places nowhere stays OPEN and hidden in the park, which is what a background tab already is -
// the tab strip only flips setVisible. Closing is the application's call, through controller.close(name).
//
// The platform's toolbar goes with the tab strip and is not attached here, so what it and the tab menu carry is on the
// controller instead - the edit mode, full screen, closing every form - and the two the platform holds the state of
// are projected, so a component can draw a chooser that shows which mode is on.
public class ReactFormsView implements FormsView {

    private final ResizableComplexPanel panel = new ResizableComplexPanel();
    private final ReactRoot root;
    private JavaScriptObject lastData; // the projection last built, so an unchanged entry can keep its reference
    private final FormsController formsController;
    private final FormsView.SelectionHandler selection;

    // where a form's view lives while nothing places it. It has to stay in the document and stay a logical child of
    // the panel, or GWT would detach the widget; the node itself must be OUTSIDE the panel, because React clears its
    // own root. display:none is what stops the form's schedulers, exactly as an unselected tab does
    private final Park park = new Park(panel);

    private final PlacedViews<String> placed = new PlacedViews<>(new PlacedViews.Views<String>() {
        @Override
        public PlacedViews.Problem problem(String name) {
            return byName.get(name) != null ? null : new PlacedViews.Problem("'" + name + "' is not an open form",
                    "'" + name + "' is not an open form, so nothing will ever be placed here");
        }

        @Override
        public boolean place(String name, Element host) {
            FormDockable dockable = byName.get(name);
            if (dockable == null)
                return false;

            Element view = dockable.getContentWidget().getElement();
            host.appendChild(view);
            GwtClientUtils.addClassName(view, "fill-parent-flex"); // the host is a flex column by its mark (layout.css)
            dockable.getContentWidget().onResize(); // a DOM move fires nothing in GWT, and this form was never measured here
            return true;
        }

        @Override
        public void park(String name) {
            FormDockable dockable = byName.get(name);
            if (dockable != null) // still attached and alive, exactly as a background tab is
                park.put(dockable.getContentWidget());
        }

        @Override
        public void resized(String name) {
            FlexTabbedPanel.scheduleOnResize(byName.get(name).getContentWidget());
        }
    });

    private final List<FormDockable> forms;
    private final NativeStringMap<FormDockable> byName = new NativeStringMap<>();
    private int selected = -1;

    // the modes are the platform's four and never change, so the SAME array goes into every snapshot: a chooser
    // memoized on it is not redrawn when a form is opened or closed
    private final JavaScriptObject editModes = buildEditModes();

    private final FormsWindowController formsWindow; // the window this draws: its forms, and its closeAll

    public ReactFormsView(String custom, FormsController formsController, FormsWindowController formsWindow, FormsView.SelectionHandler selection) {
        this.formsController = formsController;
        this.formsWindow = formsWindow;
        this.forms = formsWindow.getForms();
        this.selection = selection;

        // the crossing BACK to the platform: the component renders the host node, we move the form's own view in. A form's
        // host holds the form's view, which the layout fills it with by its marks
        root = new ReactRoot(custom, createController(formsController.getNavigatorController().getController()), PlacedViews.placement(placed, new ReactRoot.MarkedHost()));
        // no form is open yet, and that is a state the component draws - a start page, say - so the window starts
        // from it, shaped as every later snapshot is, instead of waiting for the first form to be drawn at all
        root.updateData(buildData());

        GwtClientUtils.addClassName(panel, "forms-container");

        root.mountWhileAttached(panel);
    }

    @Override
    public Widget getView() {
        return panel;
    }

    @Override
    public void formAdded(FormDockable dockable, Integer index) {
        // a logical child of the panel, parked in the DOM until the component says where it goes
        byName.put(dockable.formName, dockable);
        park.add(dockable.getContentWidget());

        // what FlexTabBar.insertTab does for the strip: a form put in at or before the current one pushes it along.
        // Only a form put back after an async close the server did not confirm arrives at an index at all, and it
        // arrives where it was
        if (index != null && index <= selected)
            selected++;
    }

    @Override
    public void formRemoved(FormDockable dockable, int index) {
        // what FlexTabbedPanel.removeTab does for the strip: the form being removed stops being current, and an index
        // after it shifts down. FormsWindowController picks the next current one afterwards
        if (index == selected) {
            selection.unselected(selected);
            selected = -1;
        } else if (index < selected)
            selected--;

        placed.viewRemoved(dockable.formName, false); // a closed form never comes back
        byName.remove(dockable.formName);
        panel.remove(dockable.getContentWidget());
    }

    @Override
    public void setCurrent(int index) {
        if (index == selected)
            return;

        selection.unselected(selected);
        selected = index;
        selection.selected(index);
    }

    @Override
    public int getCurrent() {
        return selected;
    }

    @Override
    public void formsChanged() {
        root.updateData(buildData());
    }

    private JavaScriptObject buildData() {
        // the CHOSEN edit mode, which is the one the chooser shows and the one stored between sessions - not the force
        // mode a held modifier switches to, which getEditModeIndex looks past for the same reason. The full screen is
        // System.forms' alone: the window of another component is hidden meanwhile, its component kept, and never
        // sees it on
        JavaScriptObject data = createData(EditMode.getMode(FormsController.getEditModeIndex()).name(), editModes,
                                           formsWindow.main && formsController.isFullScreenMode());
        for (int i = 0; i < forms.size(); i++) {
            FormDockable dockable = forms.get(i);
            GFormController form = dockable.getForm();

            // until the form itself arrives the container is a placeholder: its caption is the one the open request
            // carried, and after that it is the main container's, which is where a form's caption lives
            GContainer main = form != null ? form.getForm().mainContainer : null;
            String caption = main != null ? main.caption : dockable.requestedCaption;
            String image = main != null && main.image != null ? main.image.createImageHTML() : null;
            addEntry(data, dockable.formName, dockable.getCanonicalName(), caption, image,
                    i == selected, form == null, dockable.hasBlockingForm());
        }
        return lastData = ReactRoot.reuse(lastData, data);
    }

    // called back from the component's controller. The focus entering a form is heard by the window instead - once,
    // for every kind of view, since the keyboard-current form is one for the whole page - and the window then says
    // which form it shows, the same way this does
    public void selectForm(String name) {
        FormDockable dockable = byName.get(name);
        if (dockable == null) { // a name no open form has - a typo, or one kept after the form closed. Without this the
            // index would be -1, and making "no form" current would blur the one the user is in and leave nothing current
            throw GwtClientUtils.refusal("'" + name + "' is not an open form, so it cannot be made the current one");
        }

        // the window's answer to "make this the current one", not this view's: the form asked for may be the one
        // already drawn here, and then there is nothing to select - but the keyboard can still be in another window's
        // form, and moving it is what the component asked for
        formsWindow.setCurrentForm(dockable);
    }

    public void closeForm(String name) {
        FormDockable dockable = byName.get(name);
        if (dockable == null)
            throw GwtClientUtils.refusal("'" + name + "' is not an open form, so it cannot be closed");

        // a form with another one opened on top of it cannot be closed - the tab strip says that by disabling the
        // close button, and the projection says it with `blocked`. Only the button is guarded there, though, and this
        // goes straight to the form, so without this the contract would hold only for a component that reads the flag
        if (dockable.hasBlockingForm())
            throw GwtClientUtils.refusal("form '" + name + "' is blocked by a form opened from it and cannot be closed"
                    + "; the entry's `blocked` says so before it is tried");

        dockable.closePressed(); // a request: the form leaves the projection when it is actually hidden
    }

    public void setEditMode(String mode) {
        for (EditMode editMode : EditMode.values())
            if (editMode.name().equals(mode)) {
                formsController.setEditMode(editMode);
                return;
            }

        throw GwtClientUtils.refusal("'" + mode + "' is not an edit mode; `editModes` in the projection has the ones there are");
    }

    public void toggleFullScreen() {
        formsController.switchFullScreenMode();
    }

    public void closeAll() {
        formsWindow.closeAllForms();
    }

    // the component has no standard tab strip, so it gets its toolbar and tab menu operations beside the navigator's
    private native JavaScriptObject createController(JavaScriptObject navigatorController)/*-{
        var formsView = this;
        return @lsfusion.gwt.client.form.controller.GController::extendController(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(navigatorController, {
            select: function(name) {
                formsView.@lsfusion.gwt.client.form.view.ReactFormsView::selectForm(Ljava/lang/String;)(name);
            },
            close: function(name) {
                formsView.@lsfusion.gwt.client.form.view.ReactFormsView::closeForm(Ljava/lang/String;)(name);
            },
            closeAll: function() {
                formsView.@lsfusion.gwt.client.form.view.ReactFormsView::closeAll()();
            },
            setEditMode: function(mode) {
                formsView.@lsfusion.gwt.client.form.view.ReactFormsView::setEditMode(Ljava/lang/String;)(mode);
            },
            toggleFullScreen: function() {
                formsView.@lsfusion.gwt.client.form.view.ReactFormsView::toggleFullScreen()();
            }
        });
    }-*/;

    private JavaScriptObject buildEditModes() {
        JavaScriptObject modes = createEditModes();
        for (EditMode mode : EditMode.values())
            addEditMode(modes, mode.name(), mode.getTitle(ClientMessages.Instance.get()), EditMode.getImage(mode.getIndex()).createImageHTML());
        return modes;
    }

    private native JavaScriptObject createEditModes()/*-{
        return [];
    }-*/;

    // the same caption and icon the toolbar button's popup offers the mode with
    private native void addEditMode(JavaScriptObject modes, String name, String caption, String image)/*-{
        modes.push({ name: name, caption: caption, image: image });
    }-*/;

    private native JavaScriptObject createData(String editMode, JavaScriptObject editModes, boolean fullScreen)/*-{
        return { open: [], byName: {}, editMode: editMode, editModes: editModes, fullScreen: fullScreen };
    }-*/;

    // `name` is on the entry itself, so an entry passed around alone still knows which form it is - `open` is an array
    // of names, and a name is what <Platform name/> takes. `loading` says the form has not arrived yet: its caption is the one the open request carried, and there is
    // nothing else to read from it
    private native void addEntry(JavaScriptObject data, String name, String canonicalName, String caption, String image,
                                 boolean selected, boolean loading, boolean blocked)/*-{
        data.byName[name] = { name: name, canonicalName: canonicalName, caption: caption, image: image,
                              selected: selected, loading: loading, blocked: blocked };
        data.open.push(name);
    }-*/;
}
