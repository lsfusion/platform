package lsfusion.client.base.view;

import lsfusion.client.controller.MainController;
import lsfusion.interop.form.design.ComponentDesign;

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

    public static Color getDisplayColor(Color baseColor) {
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
        return new ImageIcon(new Label().createImage(prod));
    }

    public static ImageIcon createFilteredImageIcon(ImageIcon i) {
        return createFilteredImageIcon(i, getDefaultThemePanelBackground(), SwingDefaults.getPanelBackground(), SwingDefaults.getButtonForeground());
    }

    public static void designComponent(JComponent comp, ComponentDesign design) {
        if (design.background != null) {
            comp.setBackground(getDisplayColor(design.background));
            comp.setOpaque(true);
        }

        if (design.foreground != null) {
            comp.setForeground(getDisplayColor(design.foreground));
        }
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
}
