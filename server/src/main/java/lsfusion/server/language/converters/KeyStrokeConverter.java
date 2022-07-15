package lsfusion.server.language.converters;

import lsfusion.server.language.ScriptingLogicsModule;
import org.apache.commons.beanutils.converters.AbstractConverter;

import javax.swing.*;

/**
 * Converts String to KeyStrokeOptions (keyStroke, bindingModesMap, priority)
 */
public class KeyStrokeConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) {
        return ScriptingLogicsModule.parseKeyStrokeOptions(value.toString());
    }

    @Override
    protected Class getDefaultType() {
        return KeyStroke.class;
    }
}
