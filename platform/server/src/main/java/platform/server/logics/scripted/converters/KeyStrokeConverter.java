package platform.server.logics.scripted.converters;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.converters.AbstractConverter;

import javax.swing.*;

/**
 * <p>Конвертирует значение в значение типа KeyStroke. </p>
 * <p>Переданное значение конвертируется в строку, затем она парсится в KeyStroke при помощи {@link java.awt.AWTKeyStroke#getAWTKeyStroke(String)}.</p>
 * <p>Примеры:</p>
 * <p>"control DELETE" => getAWTKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK);</p>
 * <p>"alt shift X" => getAWTKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK);</p>
 */
public class KeyStrokeConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) throws Throwable {
        try {
            return KeyStroke.getAWTKeyStroke(value.toString());
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    @Override
    protected Class getDefaultType() {
        return KeyStroke.class;
    }
}
