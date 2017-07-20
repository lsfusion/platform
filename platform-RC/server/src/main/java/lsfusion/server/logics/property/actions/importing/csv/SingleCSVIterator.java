package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.SingleImportFormIterator;

import java.util.*;

class SingleCSVIterator extends SingleImportFormIterator {
    private Scanner reader;
    private Map<String, List<String>> headersMap;
    private String separator;
    private boolean noHeader;
    private List<String> headers = null;

    SingleCSVIterator(Scanner reader, Map<String, List<String>> headersMap, boolean noHeader, String separator) {
        this.reader = reader;
        this.headersMap = headersMap;
        this.noHeader = noHeader;
        this.separator = separator;

    }

    @Override
    public List<Pair<String, Object>> nextRow(String key) {
        if (headers == null) {
            if (noHeader) {
                headers = headersMap.get(key);
            } else {
                List<String> line = readLine();
                if (line != null) {
                    headers = line;
                } else return null;
            }
        }

        List<Pair<String, Object>> row = new ArrayList<>();
        List<String> line = readLine();
        if (line != null) {
            if (headers != null) {
                for (int i = 0; i < headers.size(); i++) {
                    row.add(Pair.create(headers.get(i), (Object) (line.size() > i ? line.get(i) : null)));
                }
            } else return null;
            return row;
        } else return null;
    }

    private List<String> readLine() {
        if (reader.hasNextLine()) {
            String line = reader.nextLine();
            //cut BOM
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF')
                line = line.substring(1);
            return Arrays.asList(line.split("\\" + separator));
        } else return null;
    }

    @Override
    protected void release() {
        if (reader != null)
            reader.close();
    }

}