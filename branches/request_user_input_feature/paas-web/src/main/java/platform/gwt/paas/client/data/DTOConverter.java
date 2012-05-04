package platform.gwt.paas.client.data;

import paas.api.gwt.shared.dto.BasicDTO;

public interface DTOConverter<D extends BasicDTO, R extends BasicRecord> {
    public R convert(D dto);
}
