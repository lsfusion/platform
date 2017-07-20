package jasperapi;

import com.lowagie.text.pdf.BaseFont;
import lsfusion.base.ReflectionUtils;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontFamily;
import net.sf.jasperreports.engine.fonts.SimpleFontExtensionHelper;
import net.sf.jasperreports.engine.fonts.SimpleFontFace;
import net.sf.jasperreports.engine.fonts.SimpleFontFamily;
import net.sf.jasperreports.extensions.ExtensionsRegistry;
import sun.font.FontManager;
import sun.font.FontManagerFactory;
import sun.font.PhysicalFont;
import sun.font.SunFontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ReportFontExtensionsRegistry implements ExtensionsRegistry {
    private final List<String> fontFamiliesLocations;
    private List<FontFamily> fontFamilies;

    public ReportFontExtensionsRegistry(List<String> fontFamiliesLocations) {
        this.fontFamiliesLocations = fontFamiliesLocations;
    }

    @Override
    public <T> List<T> getExtensions(Class<T> extensionType) {
        if (FontFamily.class.equals(extensionType)) {
            if (fontFamilies == null) {
                fontFamilies = new ArrayList<>();

                DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
                
                if(fontFamiliesLocations != null) { // c/p from FontExtensionsRegistry
                    SimpleFontExtensionHelper fontExtensionHelper = SimpleFontExtensionHelper.getInstance();
                    
                    for (String location : fontFamiliesLocations) {
                        List<FontFamily> families = fontExtensionHelper.loadFontFamilies(context, location);
                        fontFamilies.addAll(families);
                    }
                }

                HashMap<String, SimpleFontFamily> nameToFamily = new HashMap<>();

                FontManager fm = FontManagerFactory.getInstance();
                SunFontManager sfm = fm instanceof SunFontManager ? (SunFontManager) fm : null;
                if (sfm != null) {
                    PhysicalFont[] allFonts = null;
                    try {
                        allFonts = ReflectionUtils.getPrivateMethodValue(SunFontManager.class, sfm, "getPhysicalFonts", new Class[0], new Object[0]);
                    } catch (Exception ignored) {
                    }

                    if (allFonts != null) {
                        for (PhysicalFont font : allFonts) {
                            String fontFamily = font.getFamilyName(Locale.getDefault());

                            SimpleFontFamily ff = nameToFamily.get(fontFamily);
                            if (ff == null) {
                                ff = new SimpleFontFamily(context);
                                nameToFamily.put(fontFamily, ff);
                            }

                            ff.setName(fontFamily);
                            ff.setPdfEmbedded(true);
                            ff.setPdfEncoding(BaseFont.IDENTITY_H);

                            String fontPath = null;
                            try {
                                fontPath = (String) ReflectionUtils.getPrivateFieldValue(PhysicalFont.class, font, "platName");
                            } catch (Exception ignored) {
                            }
                            if (fontPath != null) {
                                int style = font.getStyle();

                                if (((style & Font.BOLD) > 0) && ((style & Font.ITALIC) > 0)) {
                                    SimpleFontFace boldItalicFace = new SimpleFontFace(context);
                                    boldItalicFace.setPdf(fontPath);
                                    ff.setBoldItalicFace(boldItalicFace);
                                } else if ((style & Font.BOLD) > 0) {
                                    SimpleFontFace boldFace = new SimpleFontFace(context);
                                    boldFace.setPdf(fontPath);
                                    ff.setBoldFace(boldFace);
                                } else if ((style & Font.ITALIC) > 0) {
                                    SimpleFontFace italicFace = new SimpleFontFace(context);
                                    italicFace.setPdf(fontPath);
                                    ff.setItalicFace(italicFace);
                                } else {
                                    SimpleFontFace normalFace = new SimpleFontFace(context);
                                    normalFace.setPdf(fontPath);
                                    ff.setNormalFace(normalFace);
                                }
                            }
                        }
                    }
                }

                fontFamilies.addAll(nameToFamily.values());
            }
            
            @SuppressWarnings("unchecked")
            List<T> extensions = (List<T>) fontFamilies;
            return extensions;
        }
        return null;
    }
}
