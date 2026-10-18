package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChangesTest;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

// A form's controller as it is made, over a form given whole with nothing behind it - no server, no window: what its
// constructor builds is what a test reads. The React view of a grid of its own is built with the form's containers,
// before the controllers, and is sized by the grid's columns, which the form counts before it builds the layout
public class GFormControllerTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        installPageHelpers();
    }
    // what the client's page gives it and a test's page does not (webapp/static/js/utils.js): its text and element
    // helpers the layout calls as it builds the views - here doing nothing, or answering as for plain text: nothing
    // here reads a caption
    private static native void installPageHelpers() /*-{
        var noop = function () { };
        var helpers = {
            initCaptionHtmlOrText: noop, setCaptionHtmlOrText: noop, setCaptionNodeText: noop, initDataHtmlOrText: noop,
            setDataHtmlOrText: noop, clearDataHtmlOrText: noop, initHtmlOrText: noop, clearHtmlOrText: noop,
            setHtmlOrText: noop, setNodeText: noop, fixHyphens: noop, setAttributeOrStyle: noop,
            removeAttributeOrStyle: noop, setGlobalClassName: noop, setReadonlyNative: noop, setReadonlyClass: noop,
            setReadonlyHeur: noop, setReadonlyType: noop, setDisabledNative: noop, setDisabledClass: noop,
            setDisabledType: noop,
            containsHtmlTag: function () { return false; },
            hasLongStringWithoutSpacesAndHTMLTags: function () { return false; },
            createPlainObject: function () { return {}; },
            createFocusElement: function (tag) { return $doc.createElement(tag); },
            createFocusElementType: function (tag) { return $doc.createElement(tag); }
        };
        for (var name in helpers)
            if (!$wnd[name])
                $wnd[name] = helpers[name];
    }-*/;

    public void testAGridOfItsOwnIsSizedByItsColumns() {
        GForm form = new GForm();
        form.sID = "leafGrid";
        form.mainContainer = new GContainer();
        form.mainContainer.ID = 1; form.mainContainer.nativeSID = "c1"; form.mainContainer.sID = "main";
        form.mainContainer.main = true;
        layout(form.mainContainer);
        form.mainContainer.lines = 1; form.mainContainer.childrenAlignment = GFlexAlignment.START;

        GGroupObject group = new GGroupObject();
        group.ID = 10; group.nativeSID = "g10"; group.sID = "items"; group.viewType = GClassViewType.LIST;
        group.objects.add(new GObject(group, "item", 10, "item", GIntegerType.instance));
        form.groupObjects.add(group);
        GGrid grid = new GGrid();
        grid.ID = 11; grid.nativeSID = "c11"; grid.sID = "GRID(items)"; grid.groupObject = group;
        grid.custom = "LeafGrid"; grid.react = true; // GRID(items) { custom = 'LeafGrid'; }
        grid.width = -2; grid.height = -2; // its default size, as the server says, of its lines' default number
        grid.lineWidth = -1; grid.lineHeight = -1;
        grid.container = form.mainContainer; form.mainContainer.children.add(grid);
        layout(grid);
        grid.toolbar = new GToolbar(); // the design hides it, and the filters with it: nothing of them to build
        grid.toolbar.ID = 12; grid.toolbar.sID = "TOOLBARSYSTEM(items)"; grid.toolbar.visible = false;
        grid.filterControls = new GFilterControls();
        grid.filterControls.ID = 13; grid.filterControls.sID = "FILTERCONTROLS(items)";
        grid.filtersContainer = new GContainer();
        grid.filtersContainer.ID = 14; grid.filtersContainer.sID = "FILTERS(items)";
        group.grid = grid;
        column(form, 20, "price", group);
        column(form, 21, "quantity", group);
        form.initialFormChanges = GFormChangesTest.noChanges(); // it arrives with no changes

        // no window to open it in: no forms controller
        GFormController controller = new GFormController(null, null, formContainer(), form, false, "leafGrid", 0, null);

        // the grid's view as the main container lays it out: its base size along the container, a vertical one - the
        // grid's height, its rows' by its columns' (a style here keeps no calc() size to read the width from)
        Widget view = controller.getFormLayout().getBaseComponentViews().get(grid).getShowingWidget();
        while (!(view.getLayoutData() instanceof FlexPanel.WidgetLayoutData))
            view = view.getParent();
        GSize laidOut = ((FlexPanel.WidgetLayoutData) view.getLayoutData()).flex.baseFlexBasis;
        assertEquals(grid.getHeight().getString(), laidOut.getString());
    }

    // what the server gives every component of a design to be laid out by
    private static void layout(GComponent component) {
        component.span = 1;
        component.setAlignment(GFlexAlignment.STRETCH);
        component.captionAlignmentHorz = GFlexAlignment.START; component.captionAlignmentVert = GFlexAlignment.START;
    }

    private static void column(GForm form, int id, String name, GGroupObject group) {
        GPropertyDraw property = new GPropertyDraw();
        property.ID = id; property.nativeSID = "p" + id; property.sID = name; property.integrationSID = name;
        property.propertyFormName = name; property.groupObject = group; property.isList = true;
        property.valueType = GIntegerType.instance; property.valueWidth = 80; property.valueHeight = 20;
        form.propertyDraws.add(property);
    }

    private static FormContainer formContainer() {
        return new FormContainer(null, null, false, null) {
            protected void setContent(Widget widget) { }
            public GWindowFormType getWindowType() { return GModalityWindowFormType.FLOAT; }
            public void show(GAsyncFormController asyncFormController) { }
            public void hide(EndReason editFormCloseReason) { }
            public Element getContentElement() { return null; }
            public Widget getCaptionWidget() { return new Label(); }
        };
    }
}
