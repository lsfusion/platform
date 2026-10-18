package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

// WHAT A FORM'S REACT VIEW PLACES with <Platform name/> - the view of a react container (ReactContainerView), or of a
// component that is not a container whose custom is a React component (ReactBaseComponentView): given a row, the
// renderer of a platform-drawn grid property of a group whose rows the view draws, for both kinds; given none, what
// the kind places whole (placeView). Its hosts hold views of the form, which the layout styles by their marks
abstract class ReactViewPlacement implements ReactRoot.Placement {
    final GFormController formController;
    final GReactFormData.ViewState state;

    ReactViewPlacement(GFormController formController, GReactFormData.ViewState state) {
        this.formController = formController;
        this.state = state;
    }

    @Override
    public void mount(String name, Element host, JavaScriptObject row) {
        getPlacement(name, row).mount(host);
    }

    @Override
    public void unmount(String name, Element host, JavaScriptObject row) {
        getPlacement(name, row).unmount(host);
    }

    @Override
    public ReactRoot.Host getHost() {
        return new ReactRoot.MarkedHost(); // a host here holds a view of the form, which the layout styles by its marks
    }

    // ... what a name given no row places: a view of the kind's, or a mistake
    abstract PlatformPlacement placeView(String name);

    // HOW a <Platform name> places what it names, decided here, once per mount and unmount, into one kind: what the
    // name names here - a component by its design identifier, a property by the name the view has for it, o.qty -, one
    // view of it (placeView), or, for a platform-drawn grid property of a group this view draws, its renderer for the
    // row given, which has to be a row of that group (a tree's row of a group below passes, narrowed to the path down
    // to it, as a group's change(row) takes it); and a placement that can never work, its mistake shown in the host - a
    // platform-drawn grid property with no row, a row that is no row or not the group's, a row for what has one view.
    // Mount and unmount are called back by the host's ref, the cleanup the exact inverse (F1): both are given the same
    // name and row, so both get the same kind. React runs every ref detach of a commit before any attach, so a mount
    // can never race an unmount of the same child: a second live host for one name is always a duplicate, never a
    // legitimate move
    private PlatformPlacement getPlacement(String name, JavaScriptObject row) {
        GPropertyDraw property = state.getRowPlatformProperty(name);
        if (row == null) {
            if (property == null)
                return placeView(name);
            // without the row the host would wait for a single view that is never built - silently, for good
            return new FailedPlatformPlacement(new PlacedViews.Problem(
                    "'" + name + "' is drawn per row, so its <Platform> needs a row",
                    "'" + name + "' is a property the platform draws per row: its <Platform> is given the row it"
                            + " belongs to - <Platform name=\"" + name + "\" row={row}/> -, since each row has a"
                            + " renderer of its own"));
        }
        GGroupObjectValue key = GGroupObjectValue.resolveObject(row);
        if (key == null)
            return new FailedPlatformPlacement(new PlacedViews.Problem(
                    "the `row` given to '" + name + "' is not a row",
                    "the `row` passed to '" + name + "' does not identify a row: pass the row object from the projected"
                            + " data, not its key"));
        if (property == null)
            return new FailedPlatformPlacement(new PlacedViews.Problem(
                    "'" + name + "' is not a grid property this view places per row",
                    "'" + name + "' is given a row, but it names no grid property the platform draws of a group the"
                            + " React view of '" + state.scope.sID + "' draws - group, then integration name, as its column"
                            + " entry is keyed -, so nothing is drawn per row for it here"));
        GGroupObjectValue rowKey = property.groupObject.getRowKey(key);
        if (rowKey == null)
            return new FailedPlatformPlacement(new PlacedViews.Problem(
                    "the `row` given to '" + name + "' is not a row of '" + property.groupObject.getSID() + "'",
                    "the `row` passed to '" + name + "' is a row of another group: '" + name + "' is drawn per row of '"
                            + property.groupObject.getSID() + "'"));
        return new PlatformRowPlacement(formController, name, property, rowKey);
    }

    // one <Platform>'s placement, of the kind getPlacement decides
    abstract static class PlatformPlacement {
        abstract void mount(Element host);
        abstract void unmount(Element host);
    }
    // ... a renderer per row: the row says which goes in the host, and the rows panel controller of the rows holds
    // them. A row React unmounts - scrolled out, say - sends its renderer back to waiting, it is NOT dropped
    private static final class PlatformRowPlacement extends PlatformPlacement {
        final GFormController formController;
        final String name;
        final GPropertyDraw property;
        final GGroupObjectValue rowKey;

        PlatformRowPlacement(GFormController formController, String name, GPropertyDraw property, GGroupObjectValue rowKey) {
            this.formController = formController;
            this.name = name;
            this.property = property;
            this.rowKey = rowKey;
        }

        @Override
        void mount(Element host) {
            // the property, not the name JSX used: the ledger of who holds which row is the property's own
            if (formController.getRowsPanelController(property).place(rowKey, property, host) != null)
                PlacedViews.reportDuplicate(name, host); // another <Platform> already placed this property for this row
        }

        @Override
        void unmount(Element host) {
            formController.getRowsPanelController(property).unplace(rowKey, property, host);
        }
    }
    // ... and one that can never work: its mistake is shown in the host, which holds it until the host goes
    static final class FailedPlatformPlacement extends PlatformPlacement {
        final PlacedViews.Problem problem;

        FailedPlatformPlacement(PlacedViews.Problem problem) {
            this.problem = problem;
        }

        @Override
        void mount(Element host) {
            problem.show(host);
        }

        @Override
        void unmount(Element host) {
            GwtClientUtils.clearCustomViewError(host);
        }
    }
}
