package jasperapi;

import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.extensions.DefaultExtensionsRegistry;
import net.sf.jasperreports.extensions.ExtensionsRegistry;
import net.sf.jasperreports.extensions.ExtensionsRegistryFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// c/p from SimpleFontExtensionsRegistryFactory
public class ReportExtensionsRegistryFactory implements ExtensionsRegistryFactory {
    public final static String SIMPLE_FONT_FAMILIES_PROPERTY_PREFIX =
            DefaultExtensionsRegistry.PROPERTY_REGISTRY_PREFIX + "simple.font.families.";
    
    @Override
    public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) {
        List<JRPropertiesUtil.PropertySuffix> fontFamiliesProperties = JRPropertiesUtil.getProperties(properties, SIMPLE_FONT_FAMILIES_PROPERTY_PREFIX);
        List<String> fontFamiliesLocations = new ArrayList<>();
        for (Iterator<JRPropertiesUtil.PropertySuffix> it = fontFamiliesProperties.iterator(); it.hasNext();)
        {
            JRPropertiesUtil.PropertySuffix fontFamiliesProp = it.next();
            String fontFamiliesLocation = fontFamiliesProp.getValue();
            fontFamiliesLocations.add(fontFamiliesLocation);
        }

        return new ReportFontExtensionsRegistry(fontFamiliesLocations);
    }
}
