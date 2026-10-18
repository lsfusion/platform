package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PlacedViews;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;

// a component that is not a container whose custom is a React component (window[fn], fn = the custom): the component
// draws what this one shows of its group or its tree - a grid's or a tree's rows, a toolbar's buttons, the filter
// controls, a filter group - from the form's projection, its props { data, controller } (GReactFormData), as a react
// container's draws what it holds (ReactContainerView). It is drawn where the component stands, as the component's
// own view: its layout attributes, its SHOWIF and its class are this view's, as they are its platform view's - and a
// React view it stands in places it. It holds no view of the platform's: the renderers of a platform-drawn grid
// property of the rows it draws, placed with <Platform name row/>, are held by the rows (FormView
// .checkCustomReactComponents)
public class ReactBaseComponentView {

    private final ResizableComplexPanel panel = new ResizableComplexPanel();
    private final ReactRoot root;

    public ReactBaseComponentView(GFormController formController, GComponent component) {
        GReactFormData.ViewState state = formController.addReactView(component, this::updateData);
        root = new ReactRoot(component.getCustom(), state.controller, new ReactViewPlacement(formController, state) {
            // a name given no row names nothing to place here: the view places only the renderers of its rows
            @Override
            PlatformPlacement placeView(String name) {
                return new FailedPlatformPlacement(new PlacedViews.Problem(
                        "'" + name + "' is not placed by '" + component.sID + "'",
                        "the React view of '" + component.sID + "' places nothing whole, only the per-row renderers of"
                                + " the platform-drawn grid properties of the rows it draws, given a row: what is to be"
                                + " placed whole is placed by a container around it - NEW board { custom = '"
                                + component.getCustom() + "'; MOVE " + component.sID + "; }"));
            }
        });
        GwtClientUtils.addClassName(panel, "panel-react");
        panel.addAttachHandler(event -> {
            if (event.isAttached())
                root.mount(panel.getElement(), formController.isSizeFixedOnShow());
            else
                root.unmount();
        });
    }

    public Widget getView() {
        return panel;
    }

    // GReactFormData publishes this view's snapshot after applying a remote or optimistic batch
    private void updateData(JavaScriptObject data) {
        root.updateData(data);
    }
}
