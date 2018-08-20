package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.base.Pair;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;
import lsfusion.server.logics.property.actions.importing.ImportFormPlainDataActionProperty;
import net.iryndin.jdbf.reader.DbfReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportFormDBFDataActionProperty extends ImportFormPlainDataActionProperty<SingleDBFIterator> {

    private String charset;

    public ImportFormDBFDataActionProperty(LCP<?> fileProperty, FormEntity formEntity, String charset) {
        super(new ValueClass[]{}, fileProperty, formEntity);
        this.charset = charset == null ? "cp1251" : charset;
    }

    @Override
    public List<Pair<String, SingleDBFIterator>> getRootElements(Map<String, byte[]> files, Map<String, List<String>> headersMap) throws IOException {
        List<Pair<String, SingleDBFIterator>> rootElements = new ArrayList<>();
        for(Map.Entry<String, byte[]> fileEntry : files.entrySet()) {
            rootElements.add(Pair.create(fileEntry.getKey(), new SingleDBFIterator(new DbfReader(new ByteArrayInputStream(fileEntry.getValue())), charset)));
        }
        return rootElements;
    }

    @Override
    public ImportFormIterator getIterator(List<Pair<String, SingleDBFIterator>> rootElements) {
        return new ImportFormDBFIterator(rootElements);
    }

    @Override
    public String getChildValue(Object child) {
        return child instanceof String ? (String) child : null;
    }
}