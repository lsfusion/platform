package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.base.ExternalUtils;
import lsfusion.base.Pair;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import lsfusion.server.logics.property.actions.importing.ImportFormPlainDataActionProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ImportFormCSVDataActionProperty extends ImportFormPlainDataActionProperty<SingleCSVIterator> {
    private boolean noHeader;
    private String charset;
    private String separator;

    public ImportFormCSVDataActionProperty(FormEntity formEntity, boolean noHeader, String charset, String separator) {
        super(new ValueClass[]{}, formEntity);
        this.noHeader = noHeader;
        this.charset = charset == null ? ExternalUtils.defaultCSVCharset : charset;
        this.separator = separator == null ? ExternalUtils.defaultCSVSeparator : separator;
    }

    @Override
    public List<Pair<String, SingleCSVIterator>> getRootElements(Map<String, byte[]> files, Map<String, List<String>> headersMap) throws IOException {
        List<Pair<String, SingleCSVIterator>> rootElements = new ArrayList<>();
        for(Map.Entry<String, byte[]> fileEntry : files.entrySet()) {
            rootElements.add(Pair.create(fileEntry.getKey(), new SingleCSVIterator(new Scanner(new ByteArrayInputStream(fileEntry.getValue()), charset), headersMap, noHeader, separator)));
        }
        return rootElements;
    }

    @Override
    public ImportFormIterator getIterator(List<Pair<String, SingleCSVIterator>> rootElements) {
        return new ImportFormCSVIterator(rootElements);
    }

    @Override
    public String getChildValue(Object child) {
        return child instanceof String ? (String) child : null;
    }

    @Override
    protected boolean indexBased() {
        return noHeader;
    }
}