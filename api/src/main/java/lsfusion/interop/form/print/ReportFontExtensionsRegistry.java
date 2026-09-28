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

    // system fonts are the same for every registry (the platform one and the ones of projects with their own jasperreports_extension.properties), so they are read once
    private static List<SimpleFontFamily> physicalFontFamilies;

    public void addPhysicalFontFamilies(DefaultJasperReportsContext context) {
        fontFamilies.addAll(getPhysicalFontFamilies(context));
    }

    private static synchronized List<SimpleFontFamily> getPhysicalFontFamilies(DefaultJasperReportsContext context) {
        if (physicalFontFamilies == null)
            physicalFontFamilies = readPhysicalFontFamilies(context);
        return physicalFontFamilies;
    }

    private static List<SimpleFontFamily> readPhysicalFontFamilies(DefaultJasperReportsContext context) {
        HashMap<String, SimpleFontFamily> nameToFamily = new HashMap<>();
        Set<String> fontPaths = new HashSet<>();

        FontFactoryImp fontFactory = FontFactory.getFontImp();
        Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        if (allFonts != null) {
            for (Font font : allFonts) {
                // on Windows with a non-English system locale (language for non-Unicode programs) getAllFonts() also returns every face under its localized name,
                // so the style is taken from the English name, and a face already added under another name is skipped
                String englishName = font.getFontName(Locale.ENGLISH);
                String fontPath = (String) fontFactory.getFontPath(englishName);
                if (fontPath == null || !fontPaths.add(fontPath))
                    continue;

                SimpleFontFace face = createFontFace(context, font, fontPath);
                if (face == null)
                    continue;

                String fontFamily = font.getFamily();
                SimpleFontFamily ff = nameToFamily.get(fontFamily);
                if (ff == null) {
                    ff = new SimpleFontFamily(context);
                    ff.setName(fontFamily);
                    ff.setPdfEmbedded(true);
                    ff.setPdfEncoding(BaseFont.IDENTITY_H);
                    nameToFamily.put(fontFamily, ff);
                }

                List<String> fontName = Arrays.asList(englishName.toLowerCase(Locale.ENGLISH).split(" "));
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

        return new ArrayList<>(nameToFamily.values());
    }

    private static SimpleFontFace createFontFace(DefaultJasperReportsContext context, Font font, String fontPath) {
        if (fontPath.toLowerCase(Locale.ENGLISH).contains(".ttc,"))
            return new CollectionFontFace(context, font, fontPath);

        SimpleFontFace face = new SimpleFontFace(context);
        if (!setFontFaceTtf(face, fontPath))
            return null;
        face.setPdf(fontPath);
        return face;
    }

    // a face from a TrueType collection, OpenPDF path "<file>.ttc,<index>": SimpleFontFace loads only .ttf / .otf files and takes any other ttf
    // for the name of a JVM font (a "not available to the JVM" warning and the Dialog font), so the AWT font is the installed one and the path is used for PDF only
    private static class CollectionFontFace extends SimpleFontFace {
        private final Font font;

        public CollectionFontFace(DefaultJasperReportsContext context, Font font, String fontPath) {
            super(context);
            this.font = font;
            setTtf(fontPath, false);
            setPdf(fontPath);
        }

        @Override
        public String getName() {
            return font.getName();
        }

        @Override
        public Font getFont() {
            return font;
        }
    }

    private static boolean setFontFaceTtf(SimpleFontFace fontFace, String fontPath) {
        try {
            fontFace.setTtf(fontPath);
        } catch (Exception e) { // к примеру JRFontNotFoundException, если шрифт не доступен ни Jasper'у, ни JVM
            return false;
        }
        return true;
    }
}
