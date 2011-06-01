package platform.client.navigator;

public interface INavigatorController {

    void update(ClientNavigatorWindow window, ClientNavigatorElement element);

    void openForm(ClientNavigatorElement element);
}
