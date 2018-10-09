package lsfusion.server.logics.property.actions.integration.exporting.plain.csv;

import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportByteArrayPlainWriter;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainWriter;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import java.io.*;

public class ExportCSVWriter extends ExportByteArrayPlainWriter {
    CsvEscaper csvEscaper;
    private String separator;

    public ExportCSVWriter(ImOrderMap<String, Type> fieldTypes, boolean noHeader, boolean noEscape, String separator, String charset) throws IOException {
        super(fieldTypes);
        
        if(separator == null)
            separator = ExternalUtils.defaultCSVSeparator;
        if(charset == null)
            charset = ExternalUtils.defaultCSVCharset;

        this.csvEscaper = new CsvEscaper(noEscape, separator);
        this.separator = separator;
        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, charset))) {
            @Override
            public void println() {
                write("\r\n");
            }
        };
        if(!noHeader)
            writer.println(fieldTypes.keyOrderSet().toString(this.separator));
    }

    private PrintWriter writer;
    @Override
    public void writeLine(final ImMap<String, Object> row) {
        writer.println(fieldTypes.mapOrderValues(new GetKeyValue<Object, String, Type>() {
            @Override
            public Object getMapValue(String key, Type value) {
                return csvEscaper.translate(value.formatCSV(row.get(key)));
            }
        }).valuesList().toString(separator));
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

    protected void closeWriter() {
        writer.close();
    }    
}