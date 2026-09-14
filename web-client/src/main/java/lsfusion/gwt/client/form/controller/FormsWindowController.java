package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.FormsView;
import lsfusion.gwt.client.form.view.ReactFormsView;
import lsfusion.gwt.client.form.view.TabbedFormsView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.findInList;

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

    // built once the navigator has been read, which is after FormsController is - and no form can be open before that.
    // A component draws a window in the desktop web layout only, which is the rule for every window - a navigator
    // window's component is already never drawn there, since the mobile navigator view draws that window instead. Here
    // it is not just the rule: the toolbar a component replaces carries the mobile menu button, which is the only way
    // into the navigator on a phone
    public FormsWindowController(FormsController formsController, GAbstractWindow window, boolean main, Widget toolbarView) {
        this.window = window;
        this.main = main;

        FormsView.SelectionHandler selection = new FormsView.SelectionHandler() {
            @Override
            public void unselected(int index) { // unselected (but not removed)
                if(index >= 0) {
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
                    forms.get(index).onFocus(isAdding);
                    formFocusOrder.set(index, focusOrderCount++);
                    isAdding = false;
                }
            }
        };

        formsView = window.react && !MainFrame.mobile
                ? new ReactFormsView(window.custom, formsController, this, selection)
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
        formsView.setCurrent(forms.indexOf(dockable));
        formsView.formsChanged();
    }

    public FormDockable findForm(String formCanonicalName) {
        return findInList(forms, dockable -> dockable.getCanonicalName() != null && dockable.getCanonicalName().equals(formCanonicalName));
    }

    public void addDockable(FormDockable dockable, Integer index) {
        if(index != null) {
            forms.add(index, dockable);
            formFocusOrder.add(index, null);
        } else {
            forms.add(dockable);
            formFocusOrder.add(null);
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
    // open beside the new one, and the tab strip hidden while such a window holds one form reappears, so that
    // nothing open is out of reach.
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
            setCurrentForm(lastFocusedForm);
        }
    }

    // a global class the page's CSS keys on: it says whether the MAIN forms window shows anything
    private void updateFormsNotEmptyClassName() {
        if (main)
            GwtClientUtils.setGlobalClassName(!forms.isEmpty(), "forms-container-not-empty");
    }
}
