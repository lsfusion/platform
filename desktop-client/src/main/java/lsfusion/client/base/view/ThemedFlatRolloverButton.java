package lsfusion.client.base.view;

public class ThemedFlatRolloverButton extends FlatRolloverButton {
    private String iconPath;

    public ThemedFlatRolloverButton(String iconPath) {
        this(iconPath, null);
    }

    public ThemedFlatRolloverButton(String iconPath, String text) {
        super(ClientImages.get(iconPath), text);
        this.iconPath = iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
        setIcon(ClientImages.get(iconPath));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (iconPath != null) { // first call from constructor
            setIcon(ClientImages.get(iconPath));
        }
    }
}
