package lsfusion.server.language.converters;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.value.NullValueProperty;
import org.apache.commons.beanutils.converters.AbstractConverter;

import javax.swing.*;

/**
 * Converts String to KeyStrokeOptions (keyStroke, bindingModesMap, priority)
 */
public class KeyStrokeConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) {
        String code = value instanceof PropertyObjectEntity && ((PropertyObjectEntity<?>) value).property instanceof NullValueProperty ? "" : value.toString();
        return ScriptingLogicsModule.parseKeyStrokeOptions(code);
    }

    @Override
    protected Class getDefaultType() {
        return KeyStroke.class;
    }
}
