package lsfusion.server.logics.property.actions.integration.importing.plain.csv;

import lsfusion.base.RawFileData;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportMatrixIterator;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ImportCSVIterator extends ImportMatrixIterator {
    private final Scanner reader;
    private CsvUnescaper csvUnescaper;
    private final String separator;
    
    public ImportCSVIterator(final ImOrderMap<String, Type> fieldTypes, RawFileData file, String charset, boolean noHeader, boolean noEscape, String separator) throws IOException {
        super(fieldTypes, noHeader);

        InputStream inputStream = file.getInputStream();
        this.reader = charset != null ? new Scanner(inputStream, charset) : new Scanner(inputStream);
        this.separator = separator;

        this.csvUnescaper = new CsvUnescaper(noEscape, separator);
        
        finalizeInit();
    }

    private ImList<String> line;

    @Override
    protected boolean nextRow() {
        line = readLine();
        return line != null;
    }

    private ImList<String> readLine() {
        if (reader.hasNextLine()) {
            String line = reader.nextLine();

            if (!line.isEmpty() && line.charAt(0) == '\uFEFF') // cutting BOM
                line = line.substring(1);

            return ListFact.toList(line.split(Pattern.quote(separator),-1));
        } else return null;
    }

    @Override
    public void release() {
        if (reader != null)
            reader.close();
    }

    @Override
    protected Object getPropValue(Integer fieldIndex, Type type) throws ParseException {
        if (fieldIndex >= line.size())
            throw new ParseException("Column with index " + fieldIndex + " not found");
        return type.parseCSV(csvUnescaper.translate(line.get(fieldIndex)));
    }

    //modified from StringEscapeUtils
    private static class CsvUnescaper extends CharSequenceTranslator {

        private static final char CSV_QUOTE = '"';
        private static final String CSV_QUOTE_STR = String.valueOf(CSV_QUOTE);
        private char[] CSV_SEARCH_CHARS;

        public CsvUnescaper(boolean noEscape, String separator) {
            this.CSV_SEARCH_CHARS = noEscape ? new char[] {} :
                    separator.isEmpty() ? new char[] {CSV_QUOTE, CharUtils.CR, CharUtils.LF} : new char[] {separator.charAt(0), CSV_QUOTE, CharUtils.CR, CharUtils.LF};
        }

        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {

            if(index != 0) {
                throw new IllegalStateException("CsvUnescaper should never reach the [1] index");
            }

            if (input.charAt(0) != CSV_QUOTE || input.charAt(input.length() - 1) != CSV_QUOTE || (input.length() == 1 && input.charAt(0) == CSV_QUOTE)) {
                out.write(input.toString());
                return Character.codePointCount(input, 0, input.length());
            }

            // strip quotes
            final String quoteless = input.subSequence(1, input.length() - 1).toString();

            if ( StringUtils.containsAny(quoteless, CSV_SEARCH_CHARS) ) {
                // deal with escaped quotes; ie) ""
                out.write(StringUtils.replace(quoteless, CSV_QUOTE_STR + CSV_QUOTE_STR, CSV_QUOTE_STR));
            } else {
                out.write(input.toString());
            }
            return Character.codePointCount(input, 0, input.length());
        }
    }
}