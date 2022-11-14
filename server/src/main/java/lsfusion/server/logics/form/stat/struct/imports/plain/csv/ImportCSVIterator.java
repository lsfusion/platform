package lsfusion.server.logics.form.stat.struct.imports.plain.csv;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportMatrixIterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.input.BOMInputStream;

import java.io.IOException;
import java.io.InputStreamReader;

public class ImportCSVIterator extends ImportMatrixIterator {
    private CSVParser csvReader;

    public ImportCSVIterator(final ImOrderMap<String, Type> fieldTypes, RawFileData file, String charset, String wheres, boolean noHeader, boolean noEscape, String separator) throws IOException {
        super(fieldTypes, wheres, noHeader);

        InputStreamReader isReader = charset != null ? new InputStreamReader(new BOMInputStream(file.getInputStream()), charset) : new InputStreamReader(new BOMInputStream(file.getInputStream()));
        CSVFormat.Builder builder = CSVFormat.Builder.create().setDelimiter(separator);
        this.csvReader = (noEscape ? builder.setQuote(null).setEscape(null) : builder).build().parse(isReader);

        finalizeInit();
    }

    private ImList<String> line;

    @Override
    protected boolean nextRow(boolean checkWhere) {
        do {
            line = readLine();
        } while (line != null && (checkWhere && ignoreRow()));
        return line != null;
    }

    @Override
    protected Integer getRowIndex() {
        return (int) csvReader.getRecordNumber() - 1;
    }

    private ImList<String> readLine() {
        if(csvReader.iterator().hasNext()) {
            return ListFact.toList(csvReader.iterator().next().toList());
        } else {
            return null;
        }
    }

    @Override
    public void release() throws IOException {
        if (csvReader != null)
            csvReader.close();
    }

    @Override
    protected Object getPropValue(Integer fieldIndex, Type type) throws ParseException {
        if (fieldIndex >= line.size())
            throw new ParseException("Column with index " + fieldIndex + " not found");
        try {
            return type.parseCSV(line.get(fieldIndex));
        } catch (ParseException e) {
            throw ParseException.propagateWithMessage(String.format(" (row %s, column %s)", csvReader.getRecordNumber(), fieldIndex + 1), e);
        }
    }

    @Override
    protected boolean isLastValue(Integer fieldIndex) {
        return fieldIndex >= line.size();
    }
}