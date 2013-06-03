package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.ApplicationContextProvider;

import javax.swing.*;
import java.util.List;
import java.util.Collections;

public abstract class IncrementSingleListSelectionModel extends IncrementListSelectionModel<Object> implements ComboBoxModel {
    private final boolean allowNulls;

    protected IncrementSingleListSelectionModel(ApplicationContextProvider object, String field) {
        this(object, field, false);
    }

    protected IncrementSingleListSelectionModel(ApplicationContextProvider object, String field, boolean allowNulls) {
        super(object, field);
        this.allowNulls = allowNulls;
    }

    protected void updateSelectionViews() {
        fireContentsChanged(this, -1, -1);
    }

    public List<?> getList() {
        List<?> singleList = getSingleList();
        if(allowNulls)
            return BaseUtils.mergeList(Collections.singletonList(null), singleList);
        else
            return singleList;
    }


    public abstract List<?> getSingleList();
}
