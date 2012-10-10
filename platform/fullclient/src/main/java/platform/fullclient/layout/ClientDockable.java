package platform.fullclient.layout;

import bibliothek.extension.gui.dock.theme.eclipse.EclipseTabDockAction;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.action.predefined.CCloseAction;
import bibliothek.gui.dock.common.intern.CDockable;
import com.jhlabs.image.PointFilter;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;

import java.awt.*;

// уничтожаемые формы
abstract class ClientDockable extends DefaultMultipleCDockable {

    private boolean focusMostRecentOnClose = true;

    private String formSID;
    private Component contentComponent;
    private boolean defaultComponentFocused = false;

    private LockableUI contentLayerUI;
    private JXLayer contentLayer;
    private final CustomCloseAction closeAction;

    protected ClientDockable(String formSID, DockableManager dockableManager) {
        super(dockableManager.getDockableFactory());

        this.formSID = formSID;

        setMinimizable(true);
        setMaximizable(true);
        setExternalizable(false);
        setRemoveOnClose(true);
        setCloseable(true);

        putAction(ACTION_KEY_CLOSE, closeAction = new CustomCloseAction(dockableManager.getControl()));
    }

    @Override
    public Component getFocusComponent() {
        return defaultComponentFocused
               ? null
               : contentComponent.getFocusCycleRootAncestor().getFocusTraversalPolicy().getDefaultComponent((Container) contentComponent);
    }

    public String getFormSID() {
        return formSID;
    }

    public boolean isFocusMostRecentOnClose() {
        return focusMostRecentOnClose;
    }

    public void setFocusMostRecentOnClose(boolean focusMostRecentOnClose) {
        this.focusMostRecentOnClose = focusMostRecentOnClose;
    }

    protected void setContent(String caption, Component contentComponent) {
        this.contentComponent = contentComponent;
        this.contentLayerUI = new ShadowLayerUI();
        this.contentLayer = new JXLayer(contentComponent, contentLayerUI);

        getContentPane().add(contentLayer);

        setTitleText(caption);
    }

    public void blockView() {
        closeAction.setEnabled(false);
        contentLayerUI.setLocked(true);
    }

    public void unblockView() {
        closeAction.setEnabled(true);
        contentLayerUI.setLocked(false);
        toFront();
    }

    public void onClosing() {
        setVisible(false);
    }

    // закрываются пользователем
    public void onClosed() {
        getContentPane().removeAll();
    }

    private final static class ShadowLayerUI extends LockableUI {
        public static final double opacity = 0.5;

        public ShadowLayerUI() {
            super(new BufferedImageOpEffect(
                    new PointFilter() {
                        @Override
                        public int filterRGB(int x, int y, int rgb) {
                            int a = rgb & 0xff000000;
                            int r = (rgb >> 16) & 0xff;
                            int g = (rgb >> 8) & 0xff;
                            int b = rgb & 0xff;
                            r = (int) (r * opacity);
                            g = (int) (g * opacity);
                            b = (int) (b * opacity);
                            return a | (r << 16) | (g << 8) | b;
                        }
                    }
            ));
        }
    }

    @EclipseTabDockAction
    private class CustomCloseAction extends CCloseAction {
        public CustomCloseAction(CControl control) {
            super(control);
        }

        @Override
        public void close(CDockable dockable) {
            onClosing();
        }
    }
}
