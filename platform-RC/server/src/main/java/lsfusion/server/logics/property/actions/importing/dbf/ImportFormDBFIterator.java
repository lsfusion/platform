package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.base.Pair;
import lsfusion.server.logics.property.actions.importing.ImportFormIterator;

import java.util.Iterator;
import java.util.List;

public class ImportFormDBFIterator extends ImportFormIterator {

    private Iterator<Pair<String, SingleDBFIterator>> rootIterator;
    private Pair<String, SingleDBFIterator> currentSingleIterator;
    private List<Pair<String, Object>> currentRow;
    private int i;

    ImportFormDBFIterator(List<Pair<String, SingleDBFIterator>> rootElements) {
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