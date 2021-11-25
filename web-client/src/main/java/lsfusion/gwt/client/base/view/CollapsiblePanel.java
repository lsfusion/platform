package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

public class CollapsiblePanel extends CaptionPanel implements ColorThemeChangeListener {
    private final String COLLAPSE_IMAGE_PATH = "tree_open.png";
    private final String UNFOLD_IMAGE_PATH = "tree_closed.png";

    protected GImage collapseImage;
    
    private boolean collapsed = false;

    private GFormController formController;
    private GContainer container;
    
    public CollapsiblePanel(GFormController formController, GContainer container) {
        super(container.caption);
        this.formController = formController;
        this.container = container;

        collapseImage = new GImage(COLLAPSE_IMAGE_PATH);
        collapseImage.addStyleName("collapsePanelImage");
        collapseImage.addClickHandler(event -> toggleCollapsed());
        legendWrapper.add(collapseImage, 0, GFlexAlignment.CENTER);

        MainFrame.addColorThemeChangeListener(this);
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        collapseImage.setImagePath(collapsed ? UNFOLD_IMAGE_PATH : COLLAPSE_IMAGE_PATH);

        for (int i = 1; i < getChildren().size(); i++) {
            getChildren().get(i).setVisible(!collapsed);
        }

        if (formController != null) {
            assert container != null;
            formController.setContainerCollapsed(container, collapsed);
        }
    }

    @Override
    public void colorThemeChanged() {
        collapseImage.setImagePath(collapsed ? UNFOLD_IMAGE_PATH : COLLAPSE_IMAGE_PATH);
    }
}
