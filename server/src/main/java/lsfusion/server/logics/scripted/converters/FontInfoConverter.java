package lsfusion.server.logics.scripted.converters;

import lsfusion.interop.FontInfo;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.commons.lang3.math.NumberUtils;

import java.awt.*;

/**
 * <p>Конвертирует значение в значение типа FontInfo. </p>
 * <p>Переданное значение конвертируется в строку, затем она парсится в FontInfo.</p>
 * <p>Формат строки задаётся просто перечислением слов через пробел.
 * При этом слова italic, bold - добавляют к стилю ITALIC и BOLD соответственно,
 * число интерпретируется как размер,
 * оставшееся слово - как имя шрифта.
 * При присутствии нескольких токенов для представления размера или имени - выбрасывается ConversionException. </p>
 * Имя шрифта из нескольких слов можно указывать в кавычках
 * <p>Примеры:</p>
 * <p>Tahoma bold italic 12</p>
 * <p>12 bold Tahoma</p>
 * <p>Tahoma 15</p>
 * <p>15 italic "MS Sans Serif"</p>
 * <p>37</p>
 */
public class FontInfoConverter extends AbstractConverter {
    @Override
    protected Object convertToType(Class type, Object value) throws Throwable {
        if (value instanceof Font) {
            return value;
        }

        String name = null;

        // Название шрифта состоит из нескольких слов
        String sValue = value.toString();
        if(sValue.contains("\"")) {
            int start = sValue.indexOf('"');
            int end = sValue.indexOf('"', start + 1) + 1;
            name = sValue.substring(start + 1, end - 1);
            sValue = sValue.substring(0, start) + sValue.substring(end);
        }

        int size = 0;
        boolean bold = false;
        boolean italic = false;
        for (String part : sValue.split(" ")) {
            if (part.length() == 0) {
                continue;
            }

            if (part.equalsIgnoreCase("italic")) {
                italic = true;
            } else if (part.equalsIgnoreCase("bold")) {
                bold = true;
            } else {
                int sz = NumberUtils.toInt(part, -1);
                if (sz != -1) {
                    //числовой токен

                    if (sz <= 0) {
                        throw new ConversionException("Size must be > 0");
                    }
                    if (size != 0) {
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

        return new FontInfo(name, size, bold, italic);
    }

    @Override
    protected String convertToString(Object value) throws Throwable {
        if (value instanceof FontInfo) {
            FontInfo f = (FontInfo) value;
            return f.getFontFamily()
                   + (f.isBold() ? " bold" : "")
                   + (f.isItalic() ? " italic" : "")
                   + " " + f.getFontSize();
        } else {
            return super.convertToString(value);
        }
    }

    @Override
    protected Class getDefaultType() {
        return FontInfo.class;
    }
}
