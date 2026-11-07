package lsfusion.gwt.client.navigator.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.form.controller.GFormControllerTest;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;

// a component places its buttons without the standard toolbar's tree indent; a template keeps it. No React is
// mounted in HtmlUnit: the component's <Platform>s are played through the root context, as in ReactWindowLayoutTest
public class NavigatorButtonsTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private GNavigatorWindow window;
    private final LinkedHashSet<GNavigatorElement> elements = new LinkedHashSet<>();
    private final ArrayList<GNavigatorElement> levels = new ArrayList<>();
    private GINavigatorController controller;
    private Element places;

    @Override
    protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        JsniTestSupport.installPageSetup();
        JsniTestSupport.installResizeObserver();
        GFormControllerTest.installPageHelpers();

        window = new GNavigatorWindow();
        window.canonicalName = "Buttons.window";
        for (int level = 0; level < 4; level++) {
            GNavigatorElement element = level == 3 ? new GNavigatorAction() : new GNavigatorFolder();
            element.canonicalName = "Buttons.level" + level;
            element.caption = "Level " + level;
            element.children = new ArrayList<>();
            element.window = window;
            element.parentWindow = true;
            element.platformDrawn = true;
            if (level > 0) {
                element.parent = levels.get(level - 1);
                element.parent.children.add(element);
            }
            levels.add(element);
            elements.add(element);
        }

        controller = new GINavigatorController() {
            public GNavigatorElement getRoot() { return levels.get(0); }
            public GNavigatorElement getElement(String name) {
                for (GNavigatorElement element : elements)
                    if (element.canonicalName.equals(name))
                        return element;
                return null;
            }
            public void update() { }
            public void initMobileNavigatorView(GNavigatorWindow window, Widget widget) { }
            public void activate(GNavigatorElement element, NativeEvent event) { }
            public JavaScriptObject getController() { return JavaScriptObject.createObject(); }
            public void openElement(GNavigatorAction element, NativeEvent event) { }
            public void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows) { }
        };
        places = Document.get().createDivElement();
        RootPanel.getBodyElement().appendChild(places);
    }

    @Override
    protected void gwtTearDown() {
        places.removeFromParent();
    }

    // both the constructor and change() used to give a flat row the depth classes of the standard toolbar
    public void testAComponentsPlacedButtonsHaveNoTreeIndentAfterRefresh() {
        window.custom = "Buttons";
        window.react = true;
        ReactNavigatorView react = new ReactNavigatorView(window, controller);
        react.refresh(elements, levels.get(0));

        Element[] hosts = new Element[4], buttons = new Element[4];
        for (int level = 0; level < 4; level++) {
            Element host = hosts[level] = Document.get().createDivElement();
            places.appendChild(host);
            mount(react, levels.get(level).canonicalName, host);
            assertEquals(1, host.getChildCount());
            buttons[level] = host.getFirstChildElement();
            assertLevel(buttons[level], 0);
        }

        react.refresh(elements, levels.get(1));
        for (int level = 0; level < 4; level++) {
            assertSame(buttons[level], hosts[level].getFirstChildElement());
            assertLevel(buttons[level], 0);
        }
    }

    // an HTML template still uses the toolbar's depth, on creation and when a selection updates the same buttons
    public void testATemplatesButtonsKeepTheirTreeIndentAfterRefresh() {
        StringBuilder template = new StringBuilder("<div>");
        for (GNavigatorElement element : elements)
            template.append("<Platform:").append(element.canonicalName).append(">");
        window.custom = template.append("</div>").toString();
        TemplateNavigatorView view = new TemplateNavigatorView(window, controller);
        places.appendChild(view.getView().getElement());
        view.refresh(elements, levels.get(0));

        NodeList<Element> drawn = view.getView().getElement().getElementsByTagName("a");
        assertEquals(4, drawn.getLength());
        Element[] buttons = new Element[4];
        for (int level = 0; level < 4; level++) {
            buttons[level] = drawn.getItem(level);
            assertLevel(buttons[level], level);
        }

        view.refresh(elements, levels.get(1));
        assertEquals(4, drawn.getLength());
        for (int level = 0; level < 4; level++) {
            assertSame(buttons[level], drawn.getItem(level));
            assertLevel(buttons[level], level);
        }
    }

    private void assertLevel(Element button, int level) {
        for (int other = 0; other < 4; other++)
            assertEquals(button.getClassName(), other == level, button.hasClassName("nav-link-horz-" + other));
    }

    private static native void mount(ReactNavigatorView view, String name, Element host) /*-{
        view.@lsfusion.gwt.client.navigator.view.ReactNavigatorView::root.@lsfusion.gwt.client.base.view.ReactRoot::context.view.mount(name, host);
    }-*/;
}
