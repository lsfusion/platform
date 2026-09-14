package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.FormsView;
import lsfusion.gwt.client.form.view.ReactFormsView;
import lsfusion.gwt.client.form.view.SingleFormsView;
import lsfusion.gwt.client.form.view.TabbedFormsView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.List;

// one FORMS window: the forms open in it, which of them is current, and the view that draws them - the tab strip
// of a TABBED window, the one form alone otherwise, or the React component named with WINDOW ... CUSTOM.
// System.forms is one of these, the MAIN one; every other is a WINDOW ... FORMS the application declared, which
// a form reaches with SHOW ... DOCKED <window>.
// What a window owns is where a form sits, which one is current, and the order it hands the keyboard back in.
// Everything else about an open form - its lifecycle, its container, its close - stays in FormsController, which
// is the caller for all of that; a form itself reaches its window for the two things only it knows have happened:
// it arrived, and its tab menu was asked to close them all
public class FormsWindowController {

    public final GAbstractWindow window;
    // System.forms: the layout's centre, the window a form opens in when it names none, the one whose emptiness opens
    // the navigator menu, and the one that carries the platform's toolbar. An application's own window is none of that
    public final boolean main;

    private final List<FormDockable> forms = new ArrayList<>();
    private final List<Integer> formFocusOrder = new ArrayList<>();
    private int focusOrderCount;

    private final FormsView formsView;
    private final ResizableSimplePanel container; // used for / in the setFullScreenMode, and it is assumed that it is returned in getView

    private boolean isRemoving = false;
    private boolean isAdding = false;
    // the selection is following the keyboard rather than moving it: the user put the focus in the form themselves,
    // or the keyboard is another window's and this window is only picking what it shows next
    private boolean keepFocus = false;

    // built once the navigator has been read, which is after FormsController is - and no form can be open before that.
    // A component draws a window in the desktop web layout only, which is the rule for every window - a navigator
    // window's component is already never drawn there, since the mobile navigator view draws that window instead. Here
    // it is not just the rule: what replaces the standard strip does not carry the platform's toolbar, and that toolbar
    // carries the mobile menu button, which is the only way into the navigator on a phone. The single-form view drops
    // the strip for the same reason a component does, so it falls back the same way - and one form at a time still
    // holds, since what makes the window hold one form is formArrived below, not the view
    public FormsWindowController(FormsController formsController, GAbstractWindow window, boolean main, Widget toolbarView) {
        this.window = window;
        this.main = main;

        FormsView.SelectionHandler selection = new FormsView.SelectionHandler() {
            @Override
            public void unselected(int index) { // unselected (but not removed)
                if(index >= 0 && !keepFocus) { // nothing left this form: the focus has already moved, and what it
                                               // remembers must not be overwritten with "nothing was focused"
                    FormDockable dockable = forms.get(index);
                    // the form itself, not what getCurrentForm answers: that reads as "none" while a modal popup
                    // is up, and the form under the popup holds the keyboard all the same
                    FormContainer keyboard = MainFrame.getAssertCurrentForm();

                    dockable.onBlur(isRemoving);

                    // onBlur ends by clearing the keyboard-current form, which is one for the whole page: the form
                    // this window stopped showing is not necessarily the one holding the keyboard, and when it is
                    // not - it is in another window - that one keeps it
                    if (keyboard != null && keyboard != dockable)
                        MainFrame.setCurrentForm(keyboard);

                    isRemoving = false;
                }
            }

            @Override
            public void selected(int index) {
                if(index >= 0) {
                    if (!keepFocus) // the keyboard is already where it belongs
                        forms.get(index).onFocus(isAdding);
                    formFocusOrder.set(index, focusOrderCount++);
                    isAdding = false;
                }
            }
        };

        formsView = !MainFrame.mobile && window.react ? new ReactFormsView(window.custom, formsController, this, selection)
                : !MainFrame.mobile && window.single ? new SingleFormsView(selection)
                : new TabbedFormsView(toolbarView, MainFrame.mobile, selection);

        container = new ResizableSimplePanel();
        GwtClientUtils.addClassName(container, "forms-container-window");
        container.setPercentMain(formsView.getView());
    }

    public Widget getView() {
        return container;
    }

    public List<FormDockable> getForms() {
        return forms;
    }

    public int indexOf(FormDockable dockable) {
        return forms.indexOf(dockable);
    }

    public int getFormsCount() {
        return forms.size();
    }

    // something the view shows from its projection changed - a caption, an image, the blocked state, which form is
    // current. The tab strip writes those into the tab widget as they happen; a component reads them from its
    // projection, so it is told
    public void formsChanged() {
        formsView.formsChanged();
    }

    public void setCurrentForm(FormDockable dockable) {
        int index = forms.indexOf(dockable);
        if (index < 0) // not a form this window holds: an optimistic close takes one out of its window until the
            return;    // server answers, and asking the view for it would leave the window drawing NOTHING while it
                       // still holds other forms
        if (index == formsView.getCurrent())
            focusCurrent(); // the view has nothing to change, but the keyboard may be in another window's form,
                            // and moving it is the whole point of asking for this one
        else
            formsView.setCurrent(index);

        formsView.formsChanged();
    }

    public void addDockable(FormDockable dockable, Integer index) {
        if(index != null) {
            forms.add(index, dockable);
            formFocusOrder.add(index, null);
        } else {
            forms.add(dockable);
            formFocusOrder.add(null);
        }

        // once per form, on the container that outlives every placement - and a form IS placed more than once, since an
        // async close that the server does not confirm puts the same dockable back where it was. With more than one
        // window a form can be SHOWN while another one is the keyboard-current form, and the user clicking into it is
        // the only thing that says the keyboard should move; in one window that could not happen, a form was either
        // the selected tab or hidden
        if (!dockable.focusListened) {
            dockable.focusListened = true;
            listenFocus(dockable.getContentWidget().getElement(), dockable);
        }

        updateFormsNotEmptyClassName();

        formsView.formAdded(dockable, index); // the projection is rebuilt by setCurrentForm just below

        assert !isAdding;
        isAdding = true;
        setCurrentForm(dockable);
        assert !isAdding;
    }

    public void removeDockable(FormDockable dockable) {
        int index = forms.indexOf(dockable);
        boolean wasCurrent = formsView.getCurrent() == index;

        if (wasCurrent) {
            assert !isRemoving;
            isRemoving = true;
        }

        formsView.formRemoved(dockable, index);
        assert !isRemoving; // checking that the form being closed was the current one

        forms.remove(index);
        formFocusOrder.remove(index);
        formsView.formsChanged();

        updateFormsNotEmptyClassName();

        if (forms.isEmpty() && main) {
            MainFrame.openNavigatorMenu();
        }

        ensureCurrentForm();
    }

    // a window that is not TABBED holds one form at a time, so the form that has just arrived asks every other one to close.
    // Called when the form ARRIVES, not when its container is added: an async open puts a placeholder in the window
    // long before the server confirms the form, and a form must not lose its place to a request that may still fail.
    // A close is a request - a form with unsaved changes asks the user and may stay - so a form that refuses stays
    // open beside the new one, hidden behind it, where ACTIVATE reaches it.
    // Nothing is asked of, or by, EITHER end of a docked-modal pair. A form blocked by such a child cannot close while
    // the child is open. A form that blocks its opener is there only until it closes, and whichever window it was aimed
    // at must still hold what it held when the opener gets its answer back - which is as true of a child already in the
    // window as of the one just arriving, since closing it would hand control back to a form nobody asked about.
    // Which form the window draws is what says who is displaced, and an arrival is one of the two places that can
    // change it: a form arriving into a container the window is NOT drawing stopped being drawn while it was on its
    // way - its placeholder was hidden - and no selection will ever leave it again; a form arriving as the one drawn
    // is what the forms it replaced were waiting for; and a form that is not in the window yet is about to be added
    // AND drawn, so it is that one. Two quick opens end on the second because its placeholder is the one the window
    // drew, with no order to keep
    public void formArrived(FormDockable dockable) {
        if (!displaces() || dockable.inBlockingPair())
            return;

        int index = forms.indexOf(dockable);
        if (index >= 0 && index != formsView.getCurrent())
            dockable.closePressed();
        else
            for (FormDockable other : new ArrayList<>(forms))
                if (other != dockable && !other.async && !other.inBlockingPair())
                    other.closePressed();
    }

    // whether this window displaces at all: it draws one form at a time, and it is the window that says which one.
    // A component window says nothing of the sort - what it draws is the component's to say, and it may draw several
    // forms at once, so a form it stopped SELECTING is not a form it stopped drawing. Such a window closes nothing by
    // itself; what it holds is closed through the component's own controller
    private boolean displaces() {
        return window.single && !window.react;
    }

    public void closeAllForms() {
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            private int size = forms.size();
            @Override
            public boolean execute() {
                if (MainFrame.isModalPopup())
                    return true;

                if (size > 0 && size <= forms.size()) { // check if some form not closed by user while running "closeAllForms"
                    FormDockable lastTab = forms.get(size - 1);
                    setCurrentForm(lastTab);
                    lastTab.closePressed();
                    size--;
                    return true;
                }

                return false;
            }
        }, 20);
    }

    public void ensureCurrentForm() {
        int size;
        if(formsView.getCurrent() < 0 && (size = forms.size()) > 0) {
            FormDockable lastFocusedForm = null;
            int maxOrder = 0;
            int formOrder;
            for(int i=0;i<size;i++) {
                formOrder = formFocusOrder.get(i);
                if (lastFocusedForm == null || formOrder > maxOrder) {
                    lastFocusedForm = forms.get(i);
                    maxOrder = formOrder;
                }
            }
            // what a window shows next is not the same as the keyboard: when that is another window's form - the
            // user is working there, and this window has merely lost the form it was showing - it stays there
            FormContainer keyboard = MainFrame.getAssertCurrentForm();
            keepFocus = keyboard != null && keyboard != lastFocusedForm;
            setCurrentForm(lastFocusedForm);
            keepFocus = false;
        }
    }

    // the user put the focus into this form: the keyboard follows, the way selecting a tab moves it, and this window's
    // focus order is told, so that a later ensureCurrentForm prefers it. The DOM focus itself is already where the user
    // put it, so it is left alone - onFocus would pull it to the form's default widget, away from what was clicked -
    // and so is what the form it leaves remembers, since the browser has moved on and there is no focused cell to save.
    // Which form the window SHOWS follows the click as well, and that half is the one a component view needs: it may
    // show several forms at once, and the click is what says which of them the user is in. The strip and the
    // single-form view show one, so there the form clicked into is the one already shown.
    // The keyboard does not follow when a docked-modal child has masked the form - it is not one the user can work in -
    // nor while a float or a modal popup holds it, since those do not give it up
    private void focusedIn(FormDockable dockable) {
        // a form still on its way has no form to make current; a masked one is not one the user can work in, and the
        // mask is a focus panel INSIDE it, so clicking it focuses the form; and a modal popup gives the focus it took
        // back BEFORE it stops being one, so every message box closing over a form would announce that form again
        if (!dockable.canTakeKeyboard() || MainFrame.isModalPopup())
            return;

        int index = forms.indexOf(dockable);
        assert index >= 0; // a form leaves the DOM before it leaves this list, and is put back into it before it returns
        if (index != formsView.getCurrent()) { // which form this window shows follows the click too, for a view that
            keepFocus = true;                  // shows several at once - a component one. The strip and the single-form
            formsView.setCurrent(index);       // view show one form, so for them this is already the one shown
            keepFocus = false;
            formsView.formsChanged();
        } else
            formFocusOrder.set(index, focusOrderCount++); // the selection above does this when it runs

        FormContainer current = MainFrame.getCurrentForm();
        // a float that did not take the screen - FLOAT NOWAIT - is the keyboard-current form with a form of this
        // window clickable beside it, and it keeps the keyboard while it is open
        if (current == dockable || (current != null && !(current instanceof FormDockable)))
            return;

        // lostFocus, not onBlur: this runs AFTER the browser has moved the focus, so onBlur would look for the
        // outgoing form's focused element, find none, and forget the cell the user was in - which that form needs
        // when the user comes back to it
        if (current != null && current.getForm() != null)
            current.getForm().lostFocus();

        MainFrame.setCurrentForm(dockable);
        dockable.getForm().gainedFocus();
    }

    // in the CAPTURE phase: nothing the platform draws swallows focusin, but a custom container's React component can
    // stop it in its own onFocus, and React hands that down to the native event - below this listener
    private native void listenFocus(Element view, FormDockable dockable)/*-{
        var controller = this;
        view.addEventListener('focusin', function() {
            controller.@lsfusion.gwt.client.form.controller.FormsWindowController::focusedIn(Llsfusion/gwt/client/form/view/FormDockable;)(dockable);
        }, true);
    }-*/;

    // the keyboard goes to this window's selected form, if it shows one: what a window does when the current form of
    // another window closed and nothing in that window took its place
    public boolean focusCurrent() {
        int current = formsView.getCurrent();
        if (current < 0)
            return false;
        FormDockable dockable = forms.get(current);
        if (!dockable.canTakeKeyboard())
            return false;
        if (!GwtClientUtils.isShowing(dockable.getContentWidget())) // a window HIDE WINDOW leaves undrawn still holds
            return false;                                          // its forms, and none of them can take the keyboard
        dockable.onFocus(false);
        formFocusOrder.set(current, focusOrderCount++);
        return true;
    }

    // a global class the page's CSS keys on: it says whether the MAIN forms window shows anything
    private void updateFormsNotEmptyClassName() {
        if (main)
            GwtClientUtils.setGlobalClassName(!forms.isEmpty(), "forms-container-not-empty");
    }
}
