package platform.gwt.form2.server.convert;

import platform.client.navigator.ClientNavigatorAction;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorForm;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.actions.GAction;
import platform.interop.action.ClientAction;

import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class ClientNavigatorToGwtConverter extends CachedObjectConverter {
    private static final class InstanceHolder {
        private static final ClientNavigatorToGwtConverter instance = new ClientNavigatorToGwtConverter();
    }

    public static ClientNavigatorToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private ClientNavigatorToGwtConverter() {
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
        return element;
    }

    @Cached
    @Converter(from = ClientNavigatorElement.class)
    public GNavigatorElement convertNavigatorElement(ClientNavigatorElement clientElement) {
        return initNavigatorElement(clientElement, new GNavigatorElement());
    }

    @Cached
    @Converter(from = ClientNavigatorForm.class)
    public GNavigatorElement convertNavigatorForm(ClientNavigatorForm clientForm) {
        GNavigatorElement form = initNavigatorElement(clientForm, new GNavigatorElement());
        form.icon = "form.png";
        form.isForm = true;
        return form;
    }

    @Cached
    @Converter(from = ClientNavigatorAction.class)
    public GNavigatorElement convertNavigatorAction(ClientNavigatorAction clientAction) {
        //todo:
        GNavigatorElement form = initNavigatorElement(clientAction, new GNavigatorElement());
        form.icon = "form.png";
        form.isForm = true;
        return form;
    }


}
