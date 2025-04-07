package lsfusion.server.language.converters;

import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.value.NullValueProperty;
import org.apache.commons.beanutils.converters.AbstractConverter;

import javax.swing.*;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.server.language.ScriptingLogicsModule.parseKeyStrokeOptions;

/**
 * Converts String to KeyStrokeOptions (keyStroke, bindingModesMap, priority)
 */
public class KeyStrokeConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) {
        String code = value instanceof PropertyObjectEntity && ((PropertyObjectEntity<?>) value).property instanceof NullValueProperty ? "" : value.toString();
        return parseKeyStrokeOptions(code);
    }

    @Override
    protected Class getDefaultType() {
        return KeyStroke.class;
    }

    public static InputBindingEvent parseInputBindingEvent(String value, boolean mouse) {
        if(value != null) {
            ScriptingLogicsModule.KeyStrokeOptions kso = parseKeyStrokeOptions(value);
            if (!isRedundantString(kso.keyStroke))
                return new InputBindingEvent(mouse ? new MouseInputEvent(kso.keyStroke, kso.bindingModesMap) : new KeyInputEvent(KeyStroke.getKeyStroke(kso.keyStroke), kso.bindingModesMap), kso.priority);
        }
        return null;
    }
}
