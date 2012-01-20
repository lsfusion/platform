package platform.server.logics.scripted;

import platform.interop.ClassViewType;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;

import java.util.List;

import static platform.base.BaseUtils.nvl;

public class FormPropertyOptions {
    private Boolean readOnly;
    private List<GroupObjectEntity> columns;
    private ScriptingFormEntity.MappedProperty showIf;
    private PropertyObjectEntity highlightIf;
    private PropertyObjectEntity header;
    private PropertyObjectEntity footer;
    private ClassViewType forceViewType;

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<GroupObjectEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<GroupObjectEntity> columns) {
        this.columns = columns;
    }

    public void setShowIf(ScriptingFormEntity.MappedProperty showIf) {
        this.showIf = showIf;
    }

    public ScriptingFormEntity.MappedProperty getShowIf() {
        return showIf;
    }

    public PropertyObjectEntity getHighlightIf() {
        return highlightIf;
    }

    public void setHighlightIf(PropertyObjectEntity highlightIf) {
        this.highlightIf = highlightIf;
    }

    public PropertyObjectEntity getHeader() {
        return header;
    }

    public void setHeader(PropertyObjectEntity header) {
        this.header = header;
    }

    public PropertyObjectEntity getFooter() {
        return footer;
    }

    public void setFooter(PropertyObjectEntity footer) {
        this.footer = footer;
    }

    public void setForceViewType(ClassViewType forceViewType) {
        this.forceViewType = forceViewType;
    }

    public ClassViewType getForceViewType() {
        return forceViewType;
    }

    public FormPropertyOptions overrideWith(FormPropertyOptions overrides) {
        FormPropertyOptions merged = new FormPropertyOptions();

        merged.setReadOnly(nvl(overrides.getReadOnly(), readOnly));
        merged.setColumns(nvl(overrides.getColumns(), columns));
        merged.setShowIf(nvl(overrides.getShowIf(), showIf));
        merged.setHighlightIf(nvl(overrides.getHighlightIf(), highlightIf));
        merged.setHeader(nvl(overrides.getHeader(), header));
        merged.setFooter(nvl(overrides.getFooter(), footer));
        merged.setForceViewType(nvl(overrides.getForceViewType(), forceViewType));

        return merged;
    }
}
