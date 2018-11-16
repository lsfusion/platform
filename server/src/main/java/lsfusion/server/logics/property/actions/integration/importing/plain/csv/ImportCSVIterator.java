package lsfusion.server.logics.property.actions.integration.importing.plain.csv;

import com.google.common.base.Throwables;
import lsfusion.base.RawFileData;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportMatrixIterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class ImportCSVIterator extends ImportMatrixIterator {
    private final Scanner reader;
    private final String separator;
    
    public ImportCSVIterator(final ImOrderMap<String, Type> fieldTypes, RawFileData file, String charset, boolean noHeader, String separator) throws IOException {
        super(fieldTypes, noHeader);

        InputStream inputStream = file.getInputStream();
        this.reader = charset != null ? new Scanner(inputStream, charset) : new Scanner(inputStream);
        this.separator = separator;
        
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
        return type.parseCSV(line.get(fieldIndex));
    }
}