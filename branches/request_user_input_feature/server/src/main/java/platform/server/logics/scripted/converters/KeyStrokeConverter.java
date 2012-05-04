package platform.server.logics.scripted.converters;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.converters.AbstractConverter;

import javax.swing.*;

/**
 * <p>Конвертирует значение в значение типа KeyStroke. </p>
 * <p>Переданное значение конвертируется в строку, затем она парсится в KeyStroke при помощи {@link javax.swing.KeyStroke#getKeyStroke(String)}.</p>
 * <p>Примеры:</p>
 * <p>"control DELETE" => getAWTKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK);</p>
 * <p>"alt shift X" => getAWTKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK);</p>
 */
public class KeyStrokeConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) throws Throwable {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(value.toString());
        if (keyStroke == null) {
            throw new ConversionException("Can't create KeyStroke");
        }
        return keyStroke;
    }

    @Override
    protected Class getDefaultType() {
        return KeyStroke.class;
    }
}
