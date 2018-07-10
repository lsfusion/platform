package lsfusion.server.logics.property.actions;

import lsfusion.base.ExternalUtils;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ExportCSVDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {

    private String separator;
    private boolean noHeader;
    private boolean noEscape;
    private String charset;

    public ExportCSVDataActionProperty(LocalizedString caption, String extension,
                                       ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                       ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, ImMap<String, Type> types, CalcPropertyInterfaceImplement<I> where,
                                       ImOrderMap<String, Boolean> orders, LCP targetProp, String separator, boolean noHeader, boolean noEcsape, String charset) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, types, where, orders, targetProp);

        this.separator = separator == null ? ExternalUtils.defaultCSVSeparator : separator;
        this.noHeader = noHeader;
        this.noEscape = noEcsape;
        this.charset = charset == null ? ExternalUtils.defaultCSVCharset : charset;
    }

    @Override
    protected byte[] getFile(final Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (!noHeader)
                os.write(getLineBytes(fields.toJavaList()));

            CsvEscaper csvEscaper = new CsvEscaper(noEscape, separator);
            for (ImMap<String, Object> row : rows) {
                List<String> line = new ArrayList<>();
                for (String field : fields) {
                    String cellValue = fieldTypes.getType(field).formatString(row.get(field));
                    line.add(cellValue != null ? csvEscaper.translate(cellValue) : "");
                }
                os.write(getLineBytes(line));
            }
            return os.toByteArray();
        }
    }

    private byte[] getLineBytes(List<String> line) throws UnsupportedEncodingException {
        return (StringUtils.join(line.toArray(), separator) + "\r\n").getBytes(charset);
    }

    //modified from StringEscapeUtils
    private static class CsvEscaper extends CharSequenceTranslator {

        private static final char CSV_QUOTE = '"';
        private static final String CSV_QUOTE_STR = String.valueOf(CSV_QUOTE);
        private char[] CSV_SEARCH_CHARS;

        public CsvEscaper(boolean noEscape, String separator) {
            this.CSV_SEARCH_CHARS = noEscape ? new char[] {} :
                    separator.isEmpty() ? new char[] {CSV_QUOTE, CharUtils.CR, CharUtils.LF} : new char[] {separator.charAt(0), CSV_QUOTE, CharUtils.CR, CharUtils.LF};
        }

        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {

            if(index != 0) {
                throw new IllegalStateException("CsvEscaper should never reach the [1] index");
            }

            if (StringUtils.containsNone(input.toString(), CSV_SEARCH_CHARS)) {
                out.write(input.toString());
            } else {
                out.write(CSV_QUOTE);
                out.write(StringUtils.replace(input.toString(), CSV_QUOTE_STR, CSV_QUOTE_STR + CSV_QUOTE_STR));
                out.write(CSV_QUOTE);
            }
            return Character.codePointCount(input, 0, input.length());
        }
    }
}
