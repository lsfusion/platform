package platform.gwt.form2.server.convert;

import platform.client.navigator.*;
import platform.gwt.form2.client.navigator.GNavigatorAction;
import platform.gwt.form2.client.navigator.GNavigatorForm;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.actions.GAction;
import platform.gwt.form2.shared.view.window.*;
import platform.interop.action.ClientAction;

import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class ClientNavigatorToGwtConverter extends CachedObjectConverter {
    public ClientNavigatorToGwtConverter() {
    }

    public GAction convertAction(ClientAction clientAction, Object... context) {
        return convertOrNull(clientAction, context);
    }

    public <E extends GNavigatorElement> E initNavigatorElement(ClientNavigatorElement clientElement, E element) {
        cacheInstance(clientElement, element);

        element.sid = clientElement.sID;
        element.caption = clientElement.caption;
        element.children = new ArrayList<GNavigatorElement>();
        element.icon = "open.png";
        element.isForm = false;
        for (ClientNavigatorElement child : clientElement.children) {
            GNavigatorElement childElement = convertOrCast(child);
            element.children.add(childElement);
        }
        element.window = convertOrCast(clientElement.window);
        for (ClientNavigatorElement parent : clientElement.parents) {
            element.parents.add((GNavigatorElement) convertOrCast(parent));
        }
        return element;
    }

    @Cached
    @Converter(from = ClientNavigatorElement.class)
    public GNavigatorElement convertNavigatorElement(ClientNavigatorElement clientElement) {
        return initNavigatorElement(clientElement, new GNavigatorElement());
    }

    @Cached
    @Converter(from = ClientNavigatorForm.class)
    public GNavigatorForm convertNavigatorForm(ClientNavigatorForm clientForm) {
        GNavigatorForm form = initNavigatorElement(clientForm, new GNavigatorForm());
        form.icon = "form.png";
        form.isForm = true;
        form.isPrintForm = clientForm.isPrintForm;
        form.modalityType = GModalityType.valueOf(clientForm.modalityType.name());
        return form;
    }

    @Cached
    @Converter(from = ClientNavigatorAction.class)
    public GNavigatorAction convertNavigatorAction(ClientNavigatorAction clientAction) {
        //todo:
        GNavigatorAction form = initNavigatorElement(clientAction, new GNavigatorAction());
        form.icon = "form.png";
        form.isForm = true;
        return form;
    }

    public <E extends GAbstractWindow> E initAbstractNavigatorWindow(ClientAbstractWindow clientWindow, E window) {
        cacheInstance(clientWindow, window);

        window.borderConstraint = clientWindow.borderConstraint;
        window.caption = clientWindow.caption;
        window.position = clientWindow.position;
        window.titleShown = clientWindow.titleShown;
        window.x = clientWindow.x;
        window.y = clientWindow.y;
        window.width = clientWindow.width;
        window.height = clientWindow.height;
        window.visible = clientWindow.visible;
        return window;
    }

    public <E extends GNavigatorWindow> E initNavigatorWindow(ClientNavigatorWindow clientWindow, E window) {
        initAbstractNavigatorWindow(clientWindow, window);

        window.drawRoot = clientWindow.drawRoot;
        window.drawScrollBars = clientWindow.drawScrollBars;
        window.type = clientWindow.type;
        for (ClientNavigatorElement clientElement : clientWindow.elements) {
            GNavigatorElement element = convertOrCast(clientElement, new GNavigatorElement());
            window.elements.add(element);
        }
        return window;
    }

    @Cached
    @Converter(from = ClientAbstractWindow.class)
    public GAbstractWindow convertAbstractNavigatorWindow(ClientAbstractWindow clientWindow) {
        return initAbstractNavigatorWindow(clientWindow, new GAbstractWindow());
    }

    @Cached
    @Converter(from = ClientToolBarNavigatorWindow.class)
    public GToolbarNavigatorWindow convertToolbarNavigatorWindow(ClientToolBarNavigatorWindow clientWindow) {
        GToolbarNavigatorWindow window = initNavigatorWindow(clientWindow, new GToolbarNavigatorWindow());
        window.alignmentX = clientWindow.alignmentX;
        window.alignmentY = clientWindow.alignmentY;
        window.horizontalAlignment = clientWindow.horizontalAlignment;
        window.verticalAlignment = clientWindow.verticalAlignment;
        window.horizontalTextPosition = clientWindow.horizontalTextPosition;
        window.verticalTextPosition = clientWindow.verticalTextPosition;
        window.showSelect = clientWindow.showSelect;
        window.type = clientWindow.type;
        return window;
    }

    @Cached
    @Converter(from = ClientMenuNavigatorWindow.class)
    public GMenuNavigatorWindow convertMenuNavigatorWindow(ClientMenuNavigatorWindow clientWindow) {
        GMenuNavigatorWindow window = initNavigatorWindow(clientWindow, new GMenuNavigatorWindow());
        window.orientation = clientWindow.orientation;
        window.showLevel = clientWindow.showLevel;
        return window;
    }

    @Cached
    @Converter(from = ClientPanelNavigatorWindow.class)
    public GPanelNavigatorWindow convertPanelNavigatorWindow(ClientPanelNavigatorWindow clientWindow) {
        GPanelNavigatorWindow window = initNavigatorWindow(clientWindow, new GPanelNavigatorWindow());
        window.orientation = clientWindow.orientation;
        return window;
    }

    @Cached
    @Converter(from = ClientTreeNavigatorWindow.class)
    public GTreeNavigatorWindow convertTreeNavigatorWindow(ClientTreeNavigatorWindow clientWindow) {
        return initNavigatorWindow(clientWindow, new GTreeNavigatorWindow());
    }
}
