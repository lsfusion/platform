package lsfusion.interop.form.print;

import com.lowagie.text.pdf.BaseFont;
import lsfusion.base.ReflectionUtils;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.fonts.*;
import net.sf.jasperreports.extensions.ExtensionsRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

// c/p of FontExtensionsRegistry. added addPhysicalFontFamilies() and setFontFaceTtf()
public class ReportFontExtensionsRegistry implements ExtensionsRegistry {
    private static final Log log = LogFactory.getLog(ReportFontExtensionsRegistry.class);
    
    private final List<String> fontFamiliesLocations;
    private List<FontFamily> fontFamilies;
    private List<FontSet> fontSets;

    public ReportFontExtensionsRegistry(List<String> fontFamiliesLocations) {
        this.fontFamiliesLocations = fontFamiliesLocations;
    }

    @Override
    public <T> List<T> getExtensions(Class<T> extensionType)
    {
        if (FontFamily.class.equals(extensionType))
        {
            ensureFontExtensions();

            @SuppressWarnings("unchecked")
            List<T> extensions = (List<T>) fontFamilies;
            return extensions;
        }

        if (FontSet.class.equals(extensionType))
        {
            ensureFontExtensions();

            @SuppressWarnings("unchecked")
            List<T> extensions = (List<T>) fontSets;
            return extensions;
        }

        return null;
    }

    protected void ensureFontExtensions()
    {
        if ((fontFamilies == null || fontSets == null) && fontFamiliesLocations != null)
        {
            SimpleFontExtensionHelper fontExtensionHelper = SimpleFontExtensionHelper.getInstance();
            DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();

            FontExtensionsCollector extensionsCollector = new FontExtensionsCollector();
            for (String location : fontFamiliesLocations)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Loading font extensions from " + location);
                }

                try
                {
                    fontExtensionHelper.loadFontExtensions(context, location, extensionsCollector);
                }
                catch (JRRuntimeException e)//only catching JRRuntimeException for now
                {
                    log.error("Error loading font extensions from " + location, e);
                    //keeping any font extensions collected so far, though it's a little weird
                }
            }

            fontFamilies = extensionsCollector.getFontFamilies();
            fontSets = extensionsCollector.getFontSets();
            
            addPhysicalFontFamilies(context);
        }
    }

    public void addPhysicalFontFamilies(DefaultJasperReportsContext context) {
        HashMap<String, SimpleFontFamily> nameToFamily = new HashMap<>();

        Class fontManagerFactoryClass = ReflectionUtils.classForName("sun.font.FontManagerFactory");
        Class sunFontManagerClass = ReflectionUtils.classForName("sun.font.SunFontManager");
        Class physicalFontClass = ReflectionUtils.classForName("sun.font.PhysicalFont");
        if(fontManagerFactoryClass != null && sunFontManagerClass != null) {

            Object[] allFonts = null;
            try {
                //FontManager fm = FontManagerFactory.getInstance();
                //todo: doesn't work with java 17, https://blogs.oracle.com/javamagazine/post/a-peek-into-java-17-continuing-the-drive-to-encapsulate-the-java-runtime-internals
                Object fm = ReflectionUtils.getPrivateMethodValue(fontManagerFactoryClass, null, "getInstance", new Class[0], new Object[0]);
                if (sunFontManagerClass.isInstance(fm)) {
                    allFonts = ReflectionUtils.getPrivateMethodValue(sunFontManagerClass, fm, "getPhysicalFonts", new Class[0], new Object[0]);
                }
            } catch (Exception ignored) {
            }

            if (allFonts != null) {
                for (Object font : allFonts) {

                    //String fontFamily = font.getFamilyName(Locale.getDefault());
                    String fontFamily = ReflectionUtils.getMethodValue(physicalFontClass, font, "getFamilyName", new Class[] {Locale.class}, new Object[] {Locale.getDefault()});

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
                        fontPath = (String) ReflectionUtils.getPrivateFieldValue(physicalFontClass, font, "platName");
                    } catch (Exception ignored) {
                    }
                    if (fontPath != null) {
                        int style = ReflectionUtils.getMethodValue(physicalFontClass, font, "getStyle", new Class[0], new Object[0]);

                        if (((style & Font.BOLD) > 0) && ((style & Font.ITALIC) > 0)) {
                            SimpleFontFace boldItalicFace = new SimpleFontFace(context);
                            if (setFontFaceTtf(boldItalicFace, fontPath)) {
                                boldItalicFace.setPdf(fontPath);
                                ff.setBoldItalicFace(boldItalicFace);
                            }
                        } else if ((style & Font.BOLD) > 0) {
                            SimpleFontFace boldFace = new SimpleFontFace(context);
                            if (setFontFaceTtf(boldFace, fontPath)) {
                                boldFace.setPdf(fontPath);
                                ff.setBoldFace(boldFace);
                            }
                        } else if ((style & Font.ITALIC) > 0) {
                            SimpleFontFace italicFace = new SimpleFontFace(context);
                            if (setFontFaceTtf(italicFace, fontPath)) {
                                italicFace.setPdf(fontPath);
                                ff.setItalicFace(italicFace);
                            }
                        } else {
                            SimpleFontFace normalFace = new SimpleFontFace(context);
                            if (setFontFaceTtf(normalFace, fontPath)) {
                                normalFace.setPdf(fontPath);
                                ff.setNormalFace(normalFace);
                            }
                        }
                    }
                }
            }
        }

        fontFamilies.addAll(nameToFamily.values());
    }

    private boolean setFontFaceTtf(SimpleFontFace fontFace, String fontPath) {
        try {
            fontFace.setTtf(fontPath);
        } catch (Exception e) { // к примеру JRFontNotFoundException, если шрифт не доступен ни Jasper'у, ни JVM
            return false;
        }
        return true;
    }
}
