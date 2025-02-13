package lsfusion.gwt.server.convert;

import lsfusion.client.form.property.async.ClientAsyncCloseForm;
import lsfusion.client.form.property.async.ClientAsyncOpenForm;
import lsfusion.client.navigator.*;
import lsfusion.client.navigator.window.*;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.form.property.async.GAsyncCloseForm;
import lsfusion.gwt.client.form.property.async.GAsyncOpenForm;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.window.*;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ContainerWindowFormType;
import lsfusion.interop.form.ModalityWindowFormType;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class ClientNavigatorToGwtConverter extends CachedObjectConverter {

    private final ClientNavigatorChangesToGwtConverter navigatorConverter = ClientNavigatorChangesToGwtConverter.getInstance();
    private final ClientBindingToGwtConverter bindingConverter = ClientBindingToGwtConverter.getInstance();

    public ClientNavigatorToGwtConverter(MainDispatchServlet servlet, String sessionID) {
        super(servlet, sessionID);
    }

    public GAction convertAction(ClientAction clientAction, Object... context) {
        return convertOrNull(clientAction, context);
    }

    public <E extends GNavigatorElement> E initNavigatorElement(ClientNavigatorElement clientElement, E element) throws IOException {
        cacheInstance(clientElement, element);

        element.canonicalName = clientElement.getCanonicalName();
        element.caption = clientElement.caption;
        element.elementClass = clientElement.elementClass;
        element.creationPath = clientElement.creationPath;
        element.path = clientElement.path;
        element.children = new ArrayList<>();

        if(clientElement.changeKey != null)
            element.bindingEvents.add(bindingConverter.convertBinding(clientElement.changeKey, clientElement.changeKeyPriority));
        element.showChangeKey = clientElement.showChangeKey;
        if(clientElement.changeMouse != null)
            element.bindingEvents.add(bindingConverter.convertBinding(clientElement.changeMouse, clientElement.changeMousePriority));
        element.showChangeMouse = clientElement.showChangeMouse;

        element.image = createImage(clientElement.appImage, false);

        element.asyncExec = convertOrCast(clientElement.asyncExec);

        for (ClientNavigatorElement child : clientElement.children) {
            GNavigatorElement childElement = convertOrCast(child);
            element.children.add(childElement);
        }
        element.window = convertOrCast(clientElement.window);
        element.parentWindow = clientElement.parentWindow;
        element.parent = convertOrCast(clientElement.parent);
        return element;
    }

    @Cached
    @Converter(from = ClientNavigatorAction.class)
    public GNavigatorAction convertNavigatorAction(ClientNavigatorAction clientAction) throws IOException {
        return initNavigatorElement(clientAction, new GNavigatorAction());
    }

    @Cached
    @Converter(from = ClientNavigatorFolder.class)
    public GNavigatorFolder convertNavigatorFolder(ClientNavigatorFolder clientFolder) throws IOException {
        return initNavigatorElement(clientFolder, new GNavigatorFolder());
    }
    
    public <E extends GAbstractWindow> E initAbstractNavigatorWindow(ClientAbstractWindow clientWindow, E window) {
        cacheInstance(clientWindow, window);

        window.borderConstraint = clientWindow.borderConstraint;
        window.caption = clientWindow.caption;
        window.canonicalName = clientWindow.canonicalName;
        window.position = clientWindow.position;
        window.titleShown = clientWindow.titleShown;
        window.x = clientWindow.x;
        window.y = clientWindow.y;
        window.width = clientWindow.width;
        window.height = clientWindow.height;
        window.visible = clientWindow.visible;
        window.elementClass = clientWindow.elementClass;
        window.autoSize = clientWindow.autoSize;
        return window;
    }

    public <E extends GNavigatorWindow> E initNavigatorWindow(ClientNavigatorWindow clientWindow, E window) {
        initAbstractNavigatorWindow(clientWindow, window);

        window.drawScrollBars = clientWindow.drawScrollBars;
        for (Object clientElement : clientWindow.elements) {
            GNavigatorElement element = convertOrCast((ClientNavigatorElement) clientElement);
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
    @Converter(from = ClientNavigatorWindow.class)
    public GNavigatorWindow convertToolbarNavigatorWindow(ClientNavigatorWindow clientWindow) {
        GNavigatorWindow window = initNavigatorWindow(clientWindow, new GNavigatorWindow());
        window.alignmentX = clientWindow.alignmentX;
        window.alignmentY = clientWindow.alignmentY;
        window.horizontalAlignment = clientWindow.horizontalAlignment;
        window.verticalAlignment = clientWindow.verticalAlignment;
        window.horizontalTextPosition = clientWindow.horizontalTextPosition;
        window.verticalTextPosition = clientWindow.verticalTextPosition;
        window.showSelect = clientWindow.showSelect;
        window.vertical = clientWindow.type == 1;
        return window;
    }

    @Cached
    @Converter(from = ClientNavigatorChanges.class)
    public GNavigatorChangesDTO convertNavigatorChanges(ClientNavigatorChanges clientChanges) {
        return navigatorConverter.convertOrCast(clientChanges, servlet, sessionID);
    }

    @Converter(from = ModalityWindowFormType.class)
    public GModalityWindowFormType convertModalityWindowFormType(ModalityWindowFormType modalityWindowFormType) {
        switch (modalityWindowFormType) {
            case DOCKED: return GModalityWindowFormType.DOCKED;
            case FLOAT: return GModalityWindowFormType.FLOAT;
            case EMBEDDED: return GModalityWindowFormType.EMBEDDED;
            case POPUP: return GModalityWindowFormType.POPUP;
        }
        return null;
    }

    @Converter(from = ContainerWindowFormType.class)
    public GContainerWindowFormType convertContainerWindowFormType(ContainerWindowFormType containerWindowFormType) {
        return new GContainerWindowFormType(containerWindowFormType.inContainerId);
    }

    @Cached
    @Converter(from = ClientAsyncOpenForm.class)
    public GAsyncOpenForm convertOpenForm(ClientAsyncOpenForm asyncOpenForm) throws IOException {
        GWindowFormType type = convertOrCast(asyncOpenForm.type);
        return new GAsyncOpenForm(asyncOpenForm.canonicalName, asyncOpenForm.caption, createImage(asyncOpenForm.appImage, false), asyncOpenForm.forbidDuplicate, asyncOpenForm.modal, type);
    }

    @Cached
    @Converter(from = ClientAsyncCloseForm.class)
    public GAsyncCloseForm convertCloseForm(ClientAsyncCloseForm asyncCloseForm) {
        return new GAsyncCloseForm();
    }
}
