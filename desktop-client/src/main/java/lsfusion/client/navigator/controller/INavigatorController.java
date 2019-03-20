package lsfusion.client.navigator.controller;

import lsfusion.client.navigator.ClientNavigatorElement;

public interface INavigatorController {

    void update();

    void openElement(ClientNavigatorElement element, int modifiers);
}
