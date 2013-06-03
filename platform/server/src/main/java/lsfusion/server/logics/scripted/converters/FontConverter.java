package lsfusion.server.logics.scripted.converters;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.commons.lang.math.NumberUtils;

import java.awt.*;

/**
 * <p>Конвертирует значение в значение типа Font. </p>
 * <p>Переданное значение конвертируется в строку, затем она парсится в Font.</p>
 * <p>Формат строки задаётся просто перечислением слов через пробел.
 * При этом слова italic, bold - добавляют к стилю ITALIC и BOLD соответственно,
 * число интерпретируется как размер,
 * оставшееся слово - как имя шрифта.
 * При присутствии нескольких токенов для представления размера или имени - выбрасывается ConversionException. </p>
 * <p>Примеры:</p>
 * <p>"Tahoma bold italic 12"</p>
 * <p>"12 bold Tahoma"</p>
 * <p>"Tahoma 15"</p>
 */
public class FontConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) throws Throwable {
        if (value instanceof Font) {
            return value;
        }

        String name = null;
        int style = 0;
        int size = -1;
        for (String part : value.toString().split(" ")) {
            if (part.length() == 0) {
                continue;
            }

            if (part.equalsIgnoreCase("italic")) {
                style |= Font.ITALIC;
            } else if (part.equalsIgnoreCase("bold")) {
                style |= Font.BOLD;
            } else {
                int sz = NumberUtils.toInt(part, -1);
                if (sz != -1) {
                    //числовой токен

                    if (sz < 0) {
                        throw new ConversionException("Size must be > 0");
                    }
                    if (size != -1) {
                        //уже просетали size
                        throw new ConversionException("Incorrect format: several number tokens specified");
                    }

                    size = sz;
                } else {
                    //текстовый токен
                    if (name != null) {
                        //уже просетали name
                        throw new ConversionException("Incorrect format: several name tokens specified");
                    }

                    name = part;
                }
            }
        }

        if (name == null) {
            throw new ConversionException("Incorrect format: font name isn't specified");
        }

        if (size == -1) {
            throw new ConversionException("Incorrect format: font size isn't specified");
        }

        return new Font(name, style, size);
    }

    @Override
    protected String convertToString(Object value) throws Throwable {
        if (value instanceof Font) {
            Font f = (Font) value;
            return f.getName()
                   + (f.isBold() ? " bold" : "")
                   + (f.isItalic() ? " italic" : "")
                   + " " + f.getSize();
        } else {
            return super.convertToString(value);
        }
    }

    @Override
    protected Class getDefaultType() {
        return Font.class;
    }
}
