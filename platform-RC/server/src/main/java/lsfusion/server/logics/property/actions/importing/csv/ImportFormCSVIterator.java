package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;

import java.util.Iterator;
import java.util.List;

public class ImportFormCSVIterator extends ImportFormIterator {

    private Iterator<Pair<String, SingleCSVIterator>> rootIterator;
    private Pair<String, SingleCSVIterator> currentSingleIterator;
    private List<Pair<String, Object>> currentRow;
    private int i;

    ImportFormCSVIterator(List<Pair<String, SingleCSVIterator>> rootElements) {
        this.rootIterator = rootElements.iterator();
    }

    //not used
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Pair<String, Object> next() {

        //инициализируем в начале
        if(currentSingleIterator == null && rootIterator.hasNext()) {
            currentSingleIterator = rootIterator.next();
            currentRow = currentSingleIterator.second.nextRow(currentSingleIterator.first);
            i = 0;
            return Pair.create(currentSingleIterator.first, (Object) currentSingleIterator.second);
        }

        //кончился row
        if(currentRow != null && currentRow.size() == i) {
            currentRow = currentSingleIterator.second.nextRow(currentSingleIterator.first);
            i = 0;
            if(currentRow != null)
                return Pair.create(currentSingleIterator.first, (Object) currentSingleIterator.second);
        }

        //кончился iterator, переходим на следующий
        if(currentRow == null && rootIterator.hasNext()) {
            currentSingleIterator = rootIterator.next();
            currentRow = currentSingleIterator.second.nextRow(currentSingleIterator.first);
            i = 0;
            return Pair.create(currentSingleIterator.first, (Object) currentSingleIterator.second);
        }

        return currentRow == null ? null : currentRow.get(i++);

    }

    @Override
    public void remove() {
    }
}