package platform.gwt.paas.client.pages.project;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import paas.api.gwt.shared.dto.ModuleDTO;
import platform.gwt.paas.client.data.DTOConverter;
import platform.gwt.paas.client.data.ModuleRecord;
import platform.gwt.paas.client.widgets.BasicListGrid;

public class ModuleListGrid extends BasicListGrid {

    private ProjectPageUIHandlers uiHandlers;

    public ModuleListGrid() {
        super();

        setHeight100();
        setShowRecordComponents(true);
        setShowRecordComponentsByCell(true);
        setShowHeader(false);
        //todo:  setCanReorderRecords(true);

        createFields();

        bindUIHandlers();
    }

    private void createFields() {
        ListGridField nameField = new ListGridField(ModuleRecord.NAME_FIELD);
        nameField.setEscapeHTML(true);

        ListGridField removeField = new ListGridField("removeField");
        removeField.setWidth(20);

        setFields(nameField, removeField);
    }

    @Override
    protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {
        String fieldName = this.getFieldName(colNum);

        if (fieldName.equals("removeField")) {
            HLayout recordCanvas = new HLayout(3);
            recordCanvas.setWidth100();
            recordCanvas.setHeight(20);
            recordCanvas.setAlign(Alignment.CENTER);

            ImgButton btnRemove = new ImgButton();
            btnRemove.setShowDown(false);
            btnRemove.setShowRollOver(false);
            btnRemove.setLayoutAlign(Alignment.CENTER);
            btnRemove.setSrc("icons/delete.png");
            btnRemove.setPrompt("Remove module from project");
            btnRemove.setHeight(16);
            btnRemove.setWidth(16);
            btnRemove.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    uiHandlers.removeRecordClicked((ModuleRecord) record);
                }
            });

            recordCanvas.addMember(btnRemove);

            return recordCanvas;
        } else {
            return null;
        }
    }

    private void bindUIHandlers() {
        addRecordDropHandler(new RecordDropHandler() {
            @Override
            public void onRecordDrop(RecordDropEvent event) {
                SC.say("TODO: implement reorder handling...");
            }
        });
    }

    @Override
    protected DTOConverter createDTOConverter() {
        return new DTOConverter<ModuleDTO, ModuleRecord>() {
            @Override
            public ModuleRecord convert(ModuleDTO dto) {
                return ModuleRecord.fromDTO(dto);
            }
        };
    }

    public void setUiHandlers(ProjectPageUIHandlers uiHandlers) {
        this.uiHandlers = uiHandlers;
    }
}
