package lsfusion.gwt.client.action;

import lsfusion.gwt.client.view.GColorTheme;

public class GChangeColorThemeAction extends GExecuteAction {
    public GColorTheme colorTheme;

    public GChangeColorThemeAction() {
    }

    public GChangeColorThemeAction(GColorTheme colorTheme) {
        this.colorTheme = colorTheme;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}