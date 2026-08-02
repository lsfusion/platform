package lsfusion.gwt.client.form.view;

import com.google.gwt.user.client.ui.Widget;

// where the forms open in the forms window go: the standard tab strip, or a React component the application named with
// WINDOW forms CUSTOM. The view owns WHERE a form's content sits and which one is current; everything else about a
// form - its lifecycle, its focus transition, its close - stays in FormsController, which is the view's only caller.
public interface FormsView {

    // FormsController owns the blur / focus transition; a view only says which index stopped being current and which
    // started. It stays TWO calls rather than one currentFormChanged(from, to) because the tab strip genuinely reports
    // it in two phases - FlexTabbedPanel fires before-selection with the old tab still shown and selection after the
    // switch - and the blur runs in the first of them. One call would have to be made in the second, moving the blur
    // to after the form is hidden
    interface SelectionHandler {
        void unselected(int index);
        void selected(int index);
    }


    // the same widget for the view's lifetime: WindowsController.setFullScreenMode lifts the forms window's child
    // straight into the root layout, so this must not be swapped out from under it
    Widget getView();

    void formAdded(FormDockable dockable, Integer index);

    void formRemoved(FormDockable dockable, int index);

    // make this one the current form - the view's half of what selecting a tab does; FormsController does the blur /
    // focus transition around it, through the callback it gave the view
    void setCurrent(int index);

    int getCurrent();

    // the forms the view shows, or something it shows about one of them - a caption, an image, the blocked state -
    // changed. The tab strip writes those into the tab widget as they happen and needs nothing here; a component reads
    // them from its projection, so it is told
    void formsChanged();
}
