package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GPlatformGridController;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;

// the CUSTOM view of a group's rows whose render function is a React component (ReactRoot.isReactView): what GCustom
// is to a classic render function. The component is given what the function's update is given - the rows and the
// group's OPTIONS, in its `data`, and the view's controller
public final class GReactCustom extends GTippySimpleStateTableView {
    private final ReactRoot root;

    public GReactCustom(GFormController form, GPlatformGridController grid, TableContainer tableContainer, String name) {
        super(form, grid, tableContainer);
        root = new ReactRoot(name, controller, ReactRoot.noPlacement("A group's CUSTOM component"));
        Element element = getDrawElement();
        CustomCellRenderer.setCustomElement(element);
        // drawn while the view is attached - a grid switched to another view detaches it -, and, in a form whose size
        // is fixed as it is shown, before the form is measured: a grid in CUSTOM view takes the size of what it draws
        addAttachHandler(event -> {
            if (event.isAttached())
                root.mount(element, form.isSizeFixedOnShow());
            else
                root.unmount();
        });
    }

    @Override
    protected void onUpdate(Element element, JsArray<JavaScriptObject> list) {
        root.updateData(groupData(list, getCustomOptions()));
    }

    @Override
    protected Element getCellParent(Element target) {
        return null;
    }

    private static native JavaScriptObject groupData(JsArray<JavaScriptObject> list, JavaScriptObject options) /*-{
        return {list: list, options: options};
    }-*/;
}
