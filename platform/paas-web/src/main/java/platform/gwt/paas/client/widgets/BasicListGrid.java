package platform.gwt.paas.client.widgets;

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import paas.api.gwt.shared.dto.BasicDTO;
import platform.gwt.paas.client.data.BasicRecord;
import platform.gwt.paas.client.data.DTOConverter;

public class BasicListGrid extends ListGrid {
    private DTOConverter converter;

    public BasicListGrid() {
        converter = createDTOConverter();

        setLeaveScrollbarGap(false);
        setWidth100();
        setSelectionType(SelectionStyle.SINGLE);
        setShowHeaderContextMenu(false);
        setShowHeaderMenuButton(false);
        setAnimateRemoveRecord(false);
        setShowAllRecords(true);
        setShowRollOver(false);
        setCanEdit(false);
        setAutoFitData(Autofit.VERTICAL);
        setAutoFitMaxRecords(10);
    }

    protected DTOConverter createDTOConverter() {
        return new DTOConverter() {
            @Override
            public BasicRecord convert(BasicDTO dto) {
                return new BasicRecord(dto.id);
            }
        };
    }

    public void setDataFromDTOs(BasicDTO... dtos) {
        BasicRecord selected = (BasicRecord) getSelectedRecord();
        int selectedId = selected == null ? -1 : selected.getId();

        int selectedIndex = -1;

        BasicRecord records[] = new BasicRecord[dtos.length];
        for (int i = 0; i < dtos.length; i++) {
            BasicRecord record = converter.convert(dtos[i]);
            records[i] = record;

            if (record.getId() == selectedId) {
                selectedIndex = i;
            }
        }

        setData(records);

        if (selectedIndex != -1) {
            selectSingleRecord(selectedIndex);
        }
    }

    public void removeAllRecords() {
        setData(new ListGridRecord[0]);
    }
}
