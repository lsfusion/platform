package lsfusion.client.base.view;

import lsfusion.client.view.MainFrame;

public class ThemedFlatRolloverButton extends FlatRolloverButton implements ColorThemeChangeListener {
    private String iconPath;

    public ThemedFlatRolloverButton() {
        this(null, null);
    }

    public ThemedFlatRolloverButton(String iconPath) {
        this(iconPath, null);
    }

    public ThemedFlatRolloverButton(String iconPath, String text) {
        super(ClientImages.get(iconPath), text);
        this.iconPath = iconPath;
        MainFrame.addColorThemeChangeListener(this);
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        setIcon(ClientImages.get(iconPath));
    }

    @Override
    public void colorThemeChanged() {
        setIcon(ClientImages.get(iconPath));
    }
}
