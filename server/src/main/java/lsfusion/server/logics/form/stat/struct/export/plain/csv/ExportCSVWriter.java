package lsfusion.server.logics.form.stat.struct.export.plain.csv;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportMatrixWriter;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import java.io.*;

public class ExportCSVWriter extends ExportMatrixWriter {
    private CsvEscaper csvEscaper;
    private String separator;

    public ExportCSVWriter(ImOrderMap<String, Type> fieldTypes, boolean noHeader, boolean noEscape, String separator, String charset) throws IOException {
        super(fieldTypes, noHeader);
        this.csvEscaper = new CsvEscaper(noEscape, separator);
        this.separator = separator;
        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, charset))) {
            @Override
            public void println() {
                write("\r\n");
            }
        };

        int maxIndex = -1;
        for(int index : fieldIndexMap.keyIt())
            if(index > maxIndex)
                maxIndex = index;        
        fullIndexList = ListFact.consecutiveList(maxIndex + 1, 0);

        finalizeInit();
    }

    private PrintWriter writer;

    private ImOrderSet<Integer> fullIndexList; // optimization
    
    @Override
    public void writeLine(final ImMap<String, ?> values, final ImMap<String, Type> types) {
        writer.println(fullIndexList.mapListValues(new GetValue<Object, Integer>() {
            public Object getMapValue(Integer i) {
                String field = fieldIndexMap.get(i);
                if(field == null)
                    return "";
                Type type = types.get(field);
                return csvEscaper.translate(type.formatCSV(values.get(field)));
            }
        }).toString(separator));
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