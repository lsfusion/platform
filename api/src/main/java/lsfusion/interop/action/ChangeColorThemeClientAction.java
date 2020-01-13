package lsfusion.interop.action;

import lsfusion.interop.base.view.ColorTheme;

public class ChangeColorThemeClientAction extends ExecuteClientAction {
    public ColorTheme colorTheme;
    
    public ChangeColorThemeClientAction(ColorTheme colorTheme) {
        this.colorTheme = colorTheme;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}