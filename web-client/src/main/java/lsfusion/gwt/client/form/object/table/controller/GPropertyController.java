package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GExtraLabelReader;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

// THE FORM'S CONTROLLER OF A PROPERTY, settled once where the design says who draws it
// (GFormController.propertyControllers): the platform's controller of it - a panel's, a table's - or what React has of
// one it draws or labels. All the form sends for the property goes to it - its values, what labels it, what each of its
// other readers is, its drop
public interface GPropertyController {
    boolean isPropertyShown(GPropertyDraw property);
    void focusProperty(GPropertyDraw property);
    // a change of the cell sent: it says it is loading until the answer comes, showing `value` meanwhile - its own when
    // that is PValue.UNDEFINED, there being none to show (an action's run, a value the server decides); its key, the way
    // the answer's loading is keyed, and its value before - null where the controller has no cell for it
    Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value);
    // its own values, the property being the reader of them
    void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // what labels it: its caption, image, comment, footer (GExtraLabelReader)
    void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // any other attribute it is drawn with - a reader with a field of its own (GExtraAttributeReader)
    default void updateAttribute(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        updateOther(reader, values, partial);
    }
    // any other reader of it
    void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // the server stopped showing it
    void dropProperty(GPropertyDraw property);
    // ===== its BINDINGS - keys and mouse ones (CHANGEKEY, CHANGEMOUSE) -, bound once for every property by the form,
    // whoever draws it (GFormController.initializeBindings): the widget one is taken with - while it is on the page -,
    // null where the property takes none now - by default: a controller that draws no cell of the platform's takes
    // none -, and what one does then - what the platform's binding of a cell does
    default Widget getBindingWidget(GPropertyDraw property) {
        return null;
    }
    default void onBinding(GPropertyDraw property, Event event) {
    }

    // what keeps a property React keeps nothing of - the platform's own COUNT of a list group, the pivot's, where React
    // draws the group's rows (GReactFormData.createPropertyController) -: nothing. The server sends nothing of it
    // (FormView.reactKeepsNothing); what a delta carries of it all the same is dropped, the rest of the delta applied
    GPropertyController NONE = new GPropertyController() {
        public boolean isPropertyShown(GPropertyDraw property) {
            return false;
        }
        public void focusProperty(GPropertyDraw property) {
        }
        public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
            return null;
        }
        public void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        }
        public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        }
        public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        }
        public void dropProperty(GPropertyDraw property) {
        }
    };
}
