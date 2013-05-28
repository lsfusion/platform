package platform.gwt.paas.client.pages.project;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.gwt.shared.dto.ModuleDTO;
import platform.gwt.paas.client.data.ModuleRecord;
import platform.gwt.paas.shared.actions.UpdateModulesAction;

public class ProjectPageView extends ViewWithUiHandlers<ProjectPageUIHandlers> implements ProjectPagePresenter.MyView {
    private final ProjectPageToolbar toolbar;
    private final ModulesPane modulesPane;
    private ModuleListGrid modulesGrid;

    private VLayout mainPane;
    private ImgButton refreshButton;
    private SectionStack modulesStack;

    @Inject
    public ProjectPageView(ProjectPageToolbar itoolbar, ModuleListGrid imodulesGrid, ModulesPane imodulesPane) {
        Window.enableScrolling(false);

        modulesPane = imodulesPane;
        modulesGrid = imodulesGrid;
        toolbar = itoolbar;

        setupModulesPane();

        configureLayout();

        bindUIHandlers();
    }

    private void configureLayout() {
        VLayout topPane = new VLayout();
        topPane.setAutoHeight();
        topPane.addMember(toolbar);

        HLayout leftPane = new HLayout();
        leftPane.setWidth(300);
        leftPane.setShowResizeBar(true);
        leftPane.addMember(modulesStack);

        HLayout bottomPane = new HLayout();
        bottomPane.setLayoutMargin(5);
        bottomPane.addMember(leftPane);
        bottomPane.addMember(modulesPane);

        mainPane = new VLayout();
        mainPane.setWidth100();
        mainPane.setHeight100();
        mainPane.addMember(topPane);
        mainPane.addMember(bottomPane);
    }

    private void setupModulesPane() {
        refreshButton = new ImgButton();
        refreshButton.setSrc("toolbar/refresh.png");
        refreshButton.setSize(16);
        refreshButton.setShowFocused(false);
        refreshButton.setShowRollOver(false);
        refreshButton.setShowDown(false);

        SectionStackSection modulesSection = new SectionStackSection("Project modules");
        modulesSection.setCanCollapse(false);
        modulesSection.setControls(refreshButton);
        modulesSection.setItems(modulesGrid);

        modulesStack = new SectionStack();
        modulesStack.setSections(modulesSection);
    }

    private void bindUIHandlers() {
        modulesGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
            @Override
            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
                getUiHandlers().moduleRecordSelected((ModuleRecord) event.getRecord());
            }
        });

        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                getUiHandlers().refreshModulesList();
            }
        });
    }

    @Override
    public void setUiHandlers(ProjectPageUIHandlers uiHandlers) {
        super.setUiHandlers(uiHandlers);
        toolbar.setUiHandlers(uiHandlers);
        modulesGrid.setUiHandlers(uiHandlers);
    }

    @Override
    public void openModule(ModuleRecord record) {
        modulesPane.openModule(record);
    }

    @Override
    public void setModules(ModuleDTO[] modules) {
        modulesGrid.setDataFromDTOs(modules);
        modulesPane.removeAllModulesExcept(modules);
        modulesGrid.show();
        modulesPane.show();
    }

    @Override
    public void onReveal() {
        modulesPane.cleanTabs();
        modulesGrid.removeAllRecords();
    }

    @Override
    public Widget asWidget() {
        return mainPane;
    }

    @Override
    public void setConfigurations(ConfigurationDTO[] configurations) {
        toolbar.setConfigurations(configurations);
    }

    @Override
    public void refreshContent() {
        modulesPane.refreshContent();
    }

    @Override
    public UpdateModulesAction getModulesContent() {
        return modulesPane.getModulesContent();
    }

    @Override
    public void showLoading() {
        toolbar.showLoading();
    }

    @Override
    public void hideLoading() {
        toolbar.hideLoading();
    }
}

