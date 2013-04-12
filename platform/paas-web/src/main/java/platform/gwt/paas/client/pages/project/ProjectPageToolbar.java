package platform.gwt.paas.client.pages.project;

import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import platform.gwt.paas.client.data.ConfigurationRecord;
import platform.gwt.paas.client.data.ConfigurationsDataSource;
import platform.gwt.paas.client.widgets.ToolbarWithUIHandlers;

public class ProjectPageToolbar extends ToolbarWithUIHandlers<ProjectPageUIHandlers> {

    private SelectItem cbConfigurations;
    private ToolStripButton btnStart;
    private ToolStripButton btnStop;
    private ToolStripButton btnRestart;
    private ToolStripButton btnConnect;
    private ToolStripButton btnLink;
    private DynamicForm configurationsForm;
    private ConfigurationsDataSource configurationDS;

    public ProjectPageToolbar() {
        addHomeButton();
        addSeparator();
        addToolStripButton("module_add.png", "Add module", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.addNewModuleButtonClicked();
            }
        });

        addToolStripButton("save.png", "Save all", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.saveAllButtonClicked();
            }
        });

        addToolStripButton("refresh.png", "Refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.refreshButtonClicked(true);
            }
        });

        addSeparator();

        addConfigurationsComboBox();

        btnStart = addToolStripButton("start.png", "Start configuration", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ListGridRecord selected = cbConfigurations.getSelectedRecord();
                if (selected != null) {
                    uiHandlers.startConfiguration((ConfigurationRecord) selected);
                }
            }
        });

        btnRestart = addToolStripButton("restart.png", "Restart configuration", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ListGridRecord selected = cbConfigurations.getSelectedRecord();
                if (selected != null) {
                    uiHandlers.restartConfiguration((ConfigurationRecord) selected);
                }
            }
        });

        btnStop = addToolStripButton("stop.png", "Stop configuration", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ListGridRecord selected = cbConfigurations.getSelectedRecord();
                if (selected != null) {
                    uiHandlers.stopConfiguration((ConfigurationRecord) selected);
                }
            }
        });

        btnLink = addToolStripButton("link.png", "Open in new window", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ListGridRecord selected = cbConfigurations.getSelectedRecord();
                if (selected != null) {
                    uiHandlers.openConfiguration((ConfigurationRecord) selected);
                }
            }
        });

        btnConnect = addToolStripButton("connect.png", "Download JNLP-file to connect", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ListGridRecord selected = cbConfigurations.getSelectedRecord();
                if (selected != null) {
                    uiHandlers.downloadJnlp((ConfigurationRecord) selected);
                }
            }
        });

        addToolStripButton("configuration.png", "Setup configurations", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.configurationButtonClicked();
            }
        });

        addFill();

        addLoadingIndicator();

        addConsoleButton();

        addLogoutButton();

        updateConfigButtons(null);
    }

    private void addConfigurationsComboBox() {
        configurationsForm = new DynamicForm();
        configurationsForm.setWrapItemTitles(false);
        configurationsForm.setAutoWidth();
        configurationsForm.setHeight100();
        configurationsForm.setCellPadding(3);
        configurationsForm.setNumCols(1);
        addMember(configurationsForm);

        createConfigurationCombobox();
    }

    private void createConfigurationCombobox() {
        cbConfigurations = new SelectItem("selectConfigurationItem", "Select configuration");
        cbConfigurations.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                updateConfigButtons((ConfigurationRecord) cbConfigurations.getSelectedRecord());
            }
        });

        cbConfigurations.setValueField(ConfigurationRecord.ID_FIELD);
        cbConfigurations.setDisplayField(ConfigurationRecord.NAME_FIELD);

        configurationsForm.setFields(cbConfigurations);
    }

    private void updateConfigButtons(ConfigurationRecord record) {
        btnStart.setDisabled(record == null || "started".equals(record.getStatus()));
        btnRestart.setDisabled(record == null || "stopped".equals(record.getStatus()));
        btnStop.setDisabled(record == null || "stopped".equals(record.getStatus()));
        btnLink.setDisabled(record == null || !"started".equals(record.getStatus()));
        btnConnect.setDisabled(record == null);
    }

    public void setConfigurations(ConfigurationDTO[] configurations) {
        if (configurations == null) {
            return;
        }

        ConfigurationRecord selected = (ConfigurationRecord) cbConfigurations.getSelectedRecord();
        int selectedId = selected == null ? -1 : selected.getId();

        //приходится каждый раз пересоздавать comboBox, иначе значения нормально не обновляются
        createConfigurationCombobox();

        configurationDS = new ConfigurationsDataSource(configurations);
        cbConfigurations.setOptionDataSource(configurationDS);

        if (selectedId != -1) {
            cbConfigurations.setValue(selectedId);
            updateConfigButtons(configurationDS.getRecord(selectedId));
        }
    }
}
