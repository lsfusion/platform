package platform.gwt.paas.client.data;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import paas.api.gwt.shared.dto.ModuleDTO;

public class ModuleRecord extends BasicRecord {
    public ModuleRecord() {
    }

    public ModuleRecord(int id, String name, String description) {
        super(id, name, description);
    }

    public static ModuleRecord fromDTO(ModuleDTO dto) {
        return new ModuleRecord(dto.id, dto.name, dto.description);
    }

    public static ModuleRecord[] fromDTOs(ModuleDTO[] dtos) {
        ModuleRecord records[] = new ModuleRecord[dtos.length];
        for (int i = 0; i < dtos.length; i++) {
            records[i] = ModuleRecord.fromDTO(dtos[i]);
        }
        return records;
    }

    /**
     * @param modules must contain instances of ModuleRecord
     */
    public static ModuleDTO[] toDTOs(ListGridRecord[] modules) {
        ModuleDTO dtos[] = new ModuleDTO[modules.length];
        for (int i = 0; i < modules.length; i++) {
            ModuleRecord module = (ModuleRecord) modules[i];
            dtos[i] = new ModuleDTO(
                    module.getId(),
                    module.getName(),
                    module.getDescription()
            );
        }
        return dtos;
    }
}
