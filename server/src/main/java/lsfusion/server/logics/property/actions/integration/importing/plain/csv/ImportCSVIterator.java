package lsfusion.server.logics.property.actions.integration.importing.plain.csv;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;
import lsfusion.server.logics.property.actions.integration.importing.plain.xls.ImportXLSIterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ImportCSVIterator extends ImportPlainIterator {
    private final Scanner reader;
    private final String separator;
    
    private final boolean noHeader;

    public ImportCSVIterator(final ImOrderMap<String, Type> fieldTypes, byte[] file, String charset, boolean noHeader, String separator) {
        super(fieldTypes);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
        this.reader = charset != null ? new Scanner(inputStream, charset) : new Scanner(inputStream);
        this.separator = separator;

        this.noHeader = noHeader;
        
        finalizeInit();
    }
    
    private ImMap<String, Integer> fieldIndexes;
    @Override
    protected ImOrderSet<String> readFields() {
        if(!noHeader) {
            ImList<String> line = readLine();
            if(line == null)
                return SetFact.EMPTYORDER();
            ImOrderSet<String> fields = line.toOrderExclSet();
            fieldIndexes = fields.mapOrderValues(new GetIndex<Integer>() {
                public Integer getMapValue(int i) {
                    return i;
                }
            });
            return fields;
        } else {
            ImOrderMap<String, Integer> sourceMap = ImportXLSIterator.nameToIndexColumnsMapping;
            fieldIndexes = sourceMap.getMap();
            return sourceMap.keyOrderSet();
        }
    }

    private ImList<String> line;

    @Override
    protected boolean nextRow() throws IOException {
        line = readLine();
        return line != null;
    }

    private ImList<String> readLine() {
        if (reader.hasNextLine()) {
            String line = reader.nextLine();
            //cut BOM
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF')
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
    protected Object getPropValue(String name, Type type) throws ParseException, java.text.ParseException {
        Integer fieldIndex = fieldIndexes.get(name);
        if(noHeader) { // ignoring all exceptions (just like excel)
            if (fieldIndex >= line.size())
                return null;
        }
        try {
            return type.parseCSV(line.get(fieldIndex));
        } catch (ParseException e) {
            if(!noHeader) // ignoring all exceptions (just like excel)
                throw Throwables.propagate(e);
            return null;
        }
    }
}