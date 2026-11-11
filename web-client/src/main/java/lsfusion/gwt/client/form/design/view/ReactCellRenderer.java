package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.JSValueConverter;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.Objects;

import static lsfusion.gwt.client.base.GwtClientUtils.*;

// the CUSTOM renderer of a property whose render function is a React component (ReactRoot.isReactView), where the
// platform draws the property - a cell of its grid, its value on a panel: what CustomCellRenderer is to a classic
// render function. The component draws the one cell from `data` - the cell's value and what it is drawn with, as the
// entry of a property a view draws has them (GReactFormData) - and changes it through `controller`, the property's
// member of that cell (GReactFormData.PropertyMember). It is mounted with the cell - where a React view places the
// cell, or the grid it is in, while that place holds it, as a part of that view's tree (ReactRoot.settle) - and stays
// so while a grid hands the cell another row: its data changes, and its controller answers for the row the cell shows
// when it is called
public final class ReactCellRenderer extends CellRenderer {
    private static final String CELL_VIEW = "reactCellView";
    private static final ReactRoot.Placement NO_PLACEMENT = ReactRoot.noPlacement("A property's component");

    public ReactCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        setCustomElement(element);
        CellView view = new CellView(renderContext.getForm());
        element.setPropertyObject(CELL_VIEW, view);
        element.appendChild(createHost(view.root));
        return true;
    }

    // the cell's context first, whatever the cache makes of the rest: a cell handed another row of the same value is
    // not drawn again, and its member answers for that row all the same
    @Override
    public void update(Element element, UpdateContext context) {
        ((CellView) element.getPropertyObject(CELL_VIEW)).context = context;
        super.update(element, context);
    }

    @Override
    protected Object getExtraValue(UpdateContext context) {
        return new CellAttributes(context);
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext context) {
        JavaScriptObject data = newObject();
        GReactFormData.emitPropertyValue(data, property, value);
        GReactFormData.emitPropertyFacts(data, property);
        ((CellAttributes) extraValue).emit(data);
        ((CellView) element.getPropertyObject(CELL_VIEW)).root.updateData(data);
        return false;
    }

    // the component's host has left the cell with the rest of what the cell held (clearRender), which unmounts it
    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        element.setPropertyObject(CELL_VIEW, null);
        clearCustomElement(element);
        return true;
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return PValue.getStringValue(value);
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    // what a cell is drawn with besides its value: what the cache of a cell compares to draw the cell again
    // (CustomCellRenderer.ExtraValue), and what `data` says of it
    private final class CellAttributes {
        final Boolean readOnly;
        final boolean loading;
        final String background;
        final String foreground;
        final String placeholder;
        final String defaultValue;
        final String valueTooltip;
        final String pattern;
        final String regexp;
        final String regexpMessage;
        final PValue options;

        CellAttributes(UpdateContext context) {
            // a cell of a READONLY / DISABLE property is not asked its READONLYIF, and says it is read only (GGridTable)
            readOnly = property.isReadOnly() != null ? null : context.isPropertyReadOnly();
            loading = context.isLoading();
            background = context.getBackground();
            foreground = context.getForeground();
            placeholder = context.getPlaceholder();
            defaultValue = context.getDefaultValue();
            valueTooltip = context.getValueTooltip();
            pattern = context.getPattern();
            regexp = context.getRegexp();
            regexpMessage = context.getRegexpMessage();
            options = context.getPropertyCustomOptions();
        }

        void emit(JavaScriptObject data) {
            GReactFormData.emitEditability(data, property, readOnly);
            if (loading)
                setField(data, "loading", true);
            GReactFormData.emitPresent(data, "background", background);
            GReactFormData.emitPresent(data, "foreground", foreground);
            GReactFormData.emitPresent(data, "placeholder", placeholder);
            GReactFormData.emitPresent(data, "defaultValue", defaultValue);
            GReactFormData.emitPresent(data, "valueTooltip", valueTooltip);
            GReactFormData.emitPresent(data, "pattern", pattern);
            GReactFormData.emitPresent(data, "regexp", regexp);
            GReactFormData.emitPresent(data, "regexpMessage", regexpMessage);
            GReactFormData.emitPresent(data, "options", JSValueConverter.convertToJSValue(GJSONType.instance, null, false, options));
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CellAttributes))
                return false;
            CellAttributes other = (CellAttributes) object;
            return loading == other.loading && nullEquals(readOnly, other.readOnly)
                    && nullEquals(background, other.background) && nullEquals(foreground, other.foreground)
                    && nullEquals(placeholder, other.placeholder) && nullEquals(defaultValue, other.defaultValue)
                    && nullEquals(valueTooltip, other.valueTooltip) && nullEquals(pattern, other.pattern)
                    && nullEquals(regexp, other.regexp) && nullEquals(regexpMessage, other.regexpMessage)
                    && nullEquals(options, other.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(readOnly, loading, background, foreground, placeholder, defaultValue, valueTooltip, pattern, regexp, regexpMessage, options);
        }
    }

    // what a rendered cell holds: the root its component is drawn in, the component's controller - the property's
    // member of this cell, made once - and the context of the cell's last update, which the member reads as it is called
    private final class CellView implements GReactFormData.PropertyMember {
        final GFormController form;
        final JavaScriptObject controller = GReactFormData.makePropertyMember(this, "");
        final ReactRoot root = new ReactRoot(property.customRenderFunction, controller, NO_PLACEMENT);
        UpdateContext context;

        CellView(GFormController form) {
            this.form = form;
            setField(controller, "form", form.controller); // the form's controller, beside the member's calls
        }

        public GPropertyDraw getProperty() {
            return property;
        }
        public GReactFormData.Verbs getVerbs() {
            return form;
        }
        public boolean isShown() {
            return form.getPropertyController(property).isPropertyShown(property);
        }
        // the cell is all a call can name: the key of its row in a grid, the form's current objects on a panel
        public GGroupObjectValue getMemberKey(String errorPrefix, JavaScriptObject objectOrKey) {
            if (!isUndefinedOrNull(objectOrKey))
                throw refusal(errorPrefix + "takes no row: the component draws one cell, and this is that cell's controller");
            return context.getFullKey();
        }
        public PValue getCellValue(String errorPrefix, GGroupObjectValue key) {
            return context.getValue();
        }
    }

    // the node the component is drawn in, a child of the cell's: mounted as it comes into the page and unmounted as
    // it leaves it, since no cell is told when its row or its column goes (lsfusion-custom-registry.js)
    private static native Element createHost(ReactRoot root) /*-{
        var host = $wnd.lsfusion.__createReactCellHost(
            $entry(function () { root.@lsfusion.gwt.client.base.view.ReactRoot::settle(Lcom/google/gwt/dom/client/Element;Z)(host, false); }),
            $entry(function () { root.@lsfusion.gwt.client.base.view.ReactRoot::unmount()(); })
        );
        return host;
    }-*/;
}
