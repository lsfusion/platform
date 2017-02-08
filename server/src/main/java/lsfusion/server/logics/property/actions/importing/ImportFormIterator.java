package lsfusion.server.logics.property.actions.importing;

import lsfusion.base.Pair;

import java.util.Iterator;

public abstract class ImportFormIterator implements Iterator<Pair<String, Object>> {

    @Override
    public abstract Pair<String, Object> next();

}