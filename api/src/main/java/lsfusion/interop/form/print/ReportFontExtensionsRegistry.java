package lsfusion.interop.form.print;

import com.lowagie.text.FontFactory;
import com.lowagie.text.FontFactoryImp;
import com.lowagie.text.pdf.BaseFont;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.fonts.*;
import net.sf.jasperreports.extensions.ExtensionsRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

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

        FontFactoryImp fontFactory = FontFactory.getFontImp();
        Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        if (allFonts != null) {
            for (Font font : allFonts) {
                List<String> fontName = Arrays.asList(font.getName().toLowerCase().split(" "));
                String fontFamily = font.getFamily();

                SimpleFontFamily ff = nameToFamily.get(fontFamily);
                if (ff == null) {
                    ff = new SimpleFontFamily(context);
                    ff.setName(fontFamily);
                    ff.setPdfEmbedded(true);
                    ff.setPdfEncoding(BaseFont.IDENTITY_H);
                    nameToFamily.put(fontFamily, ff);
                }

                String fontPath = (String) fontFactory.getFontPath(font.getName());
                if (fontPath != null) {
                    SimpleFontFace face = new SimpleFontFace(context);
                    if (setFontFaceTtf(face, fontPath)) {
                        face.setPdf(fontPath);
                        boolean bold = fontName.contains("bold");
                        boolean italic = fontName.contains("italic");
                        if (bold && italic) {
                            ff.setBoldItalicFace(face);
                        } else if (bold) {
                            ff.setBoldFace(face);
                        } else if (italic) {
                            ff.setItalicFace(face);
                        } else {
                            ff.setNormalFace(face);
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
