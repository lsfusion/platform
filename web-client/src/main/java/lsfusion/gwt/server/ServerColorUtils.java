package lsfusion.gwt.server;

import com.helger.commons.io.stream.HasInputStream;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.reader.CSSReader;
import lsfusion.base.Result;
import lsfusion.interop.base.view.ColorTheme;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.interop.base.view.ColorTheme.DEFAULT;

public class ServerColorUtils {
    public static Color defaultThemePanelBackground;
    public static Map<ColorTheme, Color> panelBackgrounds = new HashMap<>();
    public static Map<ColorTheme, Color> componentForegrounds = new HashMap<>();
    
    public static Color readCssColor(ColorTheme theme, String property) {
        Result<CascadingStyleSheet> rLightCss = new Result<>();
        FileUtils.readFile(FileUtils.APP_CSS_FOLDER_PATH, theme.getSid() + ".css", false, inStream -> {
            rLightCss.set(CSSReader.readFromStream(new HasInputStream(() -> inStream, false), StandardCharsets.UTF_8, ECSSVersion.CSS30));
        });
        String color = ((CSSStyleRule) rLightCss.result.getRuleAtIndex(0)).getDeclarationOfPropertyName(property).getExpression().getMemberAtIndex(0).getAsCSSString();
        return Color.decode(color);
    }
    
    public static Color getDefaultThemePanelBackground() {
        if (defaultThemePanelBackground == null) {
            defaultThemePanelBackground = readCssColor(DEFAULT, "--background-color");
        }
        return defaultThemePanelBackground; 
    }
    
    public static Color getPanelBackground(ColorTheme theme) {
        if (panelBackgrounds.containsKey(theme)) {
            return panelBackgrounds.get(theme);
        } else {
            Color panelBackground = readCssColor(theme, "--background-color");
            panelBackgrounds.put(theme, panelBackground);
            return panelBackground;
        }
    }

    public static Color getComponentForeground(ColorTheme theme) {
        if (componentForegrounds.containsKey(theme)) {
            return componentForegrounds.get(theme);
        } else {
            Color componentForeground = readCssColor(theme, "--text-color");
            componentForegrounds.put(theme, componentForeground);
            return componentForeground;
        }
    }

}
