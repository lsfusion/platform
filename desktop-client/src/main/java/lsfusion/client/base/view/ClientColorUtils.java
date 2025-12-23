package lsfusion.client.base.view;

import lsfusion.client.controller.MainController;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.interop.form.design.FontInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static lsfusion.client.base.view.SwingDefaults.getDefaultThemePanelBackground;

public class ClientColorUtils {
    public static int filterColor(int baseRGB, Color baseBackgroundColor, Color newBackgroundColor, Color customLimitColor) {
        if (baseRGB != 0) {
            Color color = new Color(baseRGB, true);
            float[] hsb = Color.RGBtoHSB(
                    max(min(baseBackgroundColor.getRed() - color.getRed() + newBackgroundColor.getRed(), customLimitColor.getRed()), 0),
                    max(min(baseBackgroundColor.getGreen() - color.getGreen() + newBackgroundColor.getGreen(), customLimitColor.getGreen()), 0),
                    max(min(baseBackgroundColor.getBlue() - color.getBlue() + newBackgroundColor.getBlue(), customLimitColor.getBlue()), 0),
                    null
            );
            Color color1 = new Color(Color.HSBtoRGB(Math.abs(0.5f + hsb[0]), hsb[1], hsb[2]));
            return new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), color.getAlpha()).getRGB();
        }

        return baseRGB;
    }

    public static Color getThemedColor(Color baseColor) {
        if (baseColor != null && !MainController.colorTheme.isDefault()) {
            return new Color(filterColor(baseColor.getRGB(),
                    SwingDefaults.getDefaultThemeTableCellBackground(),
                    SwingDefaults.getTableCellBackground(),
                    SwingDefaults.getTableCellForeground()));
        }
        return baseColor;
    }

    public static ImageIcon createFilteredImageIcon(ImageIcon i, Color baseBackgroundColor, Color newBackgroundColor, Color customLimitColor) {
        ImageProducer prod = new FilteredImageSource(i.getImage().getSource(), new ImageThemeFilter(baseBackgroundColor, newBackgroundColor, customLimitColor));
        return new ImageIcon(new FakeComponent().createImage(prod));
    }

    public static ImageIcon createFilteredImageIcon(ImageIcon i) {
        return createFilteredImageIcon(i, getDefaultThemePanelBackground(), SwingDefaults.getPanelBackground(), SwingDefaults.getButtonForeground());
    }

    public static void designComponent(JComponent comp, ClientComponent clientComponent) {
        installFont(clientComponent.font, comp);

        if (clientComponent.background != null) {
            comp.setBackground(getThemedColor(clientComponent.background));
            comp.setOpaque(true);
        }

        if (clientComponent.foreground != null) {
            comp.setForeground(getThemedColor(clientComponent.foreground));
        }
    }

    public static void installFont(FontInfo font, JComponent comp) {
        if (font != null) {
            comp.setFont(getOrDeriveComponentFont(font, comp));
        }
    }

    public static void designHeader(FontInfo captionFont, Component comp) {
        if (captionFont != null) {
            comp.setFont(getCaptionFont(captionFont, comp));
        }
    }

    public static Font getCaptionFont(FontInfo captionFont, Component component) {
        return getOrDeriveComponentFont(captionFont, component);
    }

    public static Font getOrDeriveComponentFont(FontInfo fontInfo, Component component) {
        if (fontInfo == null) {
            return component.getFont();
        }

        Object oFont = component instanceof JComponent ? ((JComponent) component).getClientProperty(fontInfo) : null;
        if (oFont instanceof Font) {
            return (Font) oFont;
        }

        Font cFont = fontInfo.deriveFrom(component);
        if (component instanceof JComponent) {
            ((JComponent) component).putClientProperty(fontInfo, cFont);
        }
        return cFont;
    }

    public static class ImageThemeFilter extends RGBImageFilter {
        private Color baseBackgroundColor;
        Color newBackgroundColor;
        Color customLimitColor;

        public ImageThemeFilter(Color baseBackgroundColor, Color newBackgroundColor, Color customLimitColor) {
            this.baseBackgroundColor = baseBackgroundColor;
            this.newBackgroundColor = newBackgroundColor;
            this.customLimitColor = customLimitColor;
        }
        
        @Override
        public int filterRGB(int x, int y, int rgb) {
            return ClientColorUtils.filterColor(rgb, baseBackgroundColor, newBackgroundColor, customLimitColor);
        }
    }
    
    private static class FakeComponent extends Component {
    }
}
