package lsfusion.gwt.server;

import com.helger.commons.functional.ISupplier;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import lsfusion.interop.base.view.ColorTheme;

import javax.servlet.ServletContext;
import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.interop.base.view.ColorTheme.DEFAULT;

public class ServerColorUtils {
    public static Color defaultThemePanelBackground;
    public static Map<ColorTheme, Color> panelBackgrounds = new HashMap<>();
    public static Map<ColorTheme, Color> componentForegrounds = new HashMap<>();
    
    public static Color readCssColor(ServletContext servletContext, ColorTheme theme, String property) {
        CascadingStyleSheet css = CSSReader.readFromStream(
                HasInputStream.once((ISupplier<InputStream>) () -> servletContext.getResourceAsStream("/" + FileUtils.STATIC_CSS_RESOURCE_PATH + theme.getSid() + ".css")),
                StandardCharsets.UTF_8,
                ECSSVersion.CSS30);
        String color = ((CSSStyleRule) css.getRuleAtIndex(0)).getDeclarationOfPropertyName(property).getExpression().getMemberAtIndex(0).getAsCSSString();
        return Color.decode(color);
    }
    
    public static Color getDefaultThemePanelBackground(ServletContext servletContext) {
        if (defaultThemePanelBackground == null) {
            defaultThemePanelBackground = readCssColor(servletContext, DEFAULT, "--background-color");
        }
        return defaultThemePanelBackground; 
    }
    
    public static Color getPanelBackground(ServletContext servletContext, ColorTheme theme) {
        if (panelBackgrounds.containsKey(theme)) {
            return panelBackgrounds.get(theme);
        } else {
            Color panelBackground = readCssColor(servletContext, theme, "--background-color");
            panelBackgrounds.put(theme, panelBackground);
            return panelBackground;
        }
    }

    public static Color getComponentForeground(ServletContext servletContext, ColorTheme theme) {
        if (componentForegrounds.containsKey(theme)) {
            return componentForegrounds.get(theme);
        } else {
            Color componentForeground = readCssColor(servletContext, theme, "--text-color");
            componentForegrounds.put(theme, componentForeground);
            return componentForeground;
        }
    }

}
