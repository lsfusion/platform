package lsfusion.gwt.form.client.form.ui.classes;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalWindow;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.shared.view.classes.GObjectClass;

public class GResizableClassDialog extends GResizableModalWindow {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private ClassTreePanel classPanel;

    private Button btnOk;
    private Button btnCancel;

    private GObjectClass chosenClass;

    public GResizableClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        super(messages.choseClass());

        this.concreate = concreate;
        this.classChosenHandler = classChosenHandler;

        configureLayout(baseClass, defaultClass);

        bindUIHandlers();
    }

    private void configureLayout(GObjectClass baseClass, GObjectClass defaultClass) {
        classPanel = new ClassTreePanel(baseClass, defaultClass) {
            @Override
            public void classChosen() {
                okPressed();
            }
        };

        btnOk = new Button(messages.ok());
        btnCancel = new Button(messages.cancel());

        FlowPanel bottomPanel = new FlowPanel();
        bottomPanel.add(btnOk);
        bottomPanel.add(btnCancel);

        ResizableVerticalPanel bottomAlignedPanel = new ResizableVerticalPanel();
        bottomAlignedPanel.setWidth("100%");
        bottomAlignedPanel.add(bottomPanel);
        bottomAlignedPanel.setCellHorizontalAlignment(bottomPanel, HasAlignment.ALIGN_RIGHT);

        DockLayoutPanel centerPanel = new DockLayoutPanel(Style.Unit.PX);
        centerPanel.addSouth(bottomAlignedPanel, 36);
        centerPanel.add(classPanel);

        ResizeLayoutPanel mainPanel = new ResizeLayoutPanel();
        mainPanel.setPixelSize(500, 500);
        mainPanel.add(centerPanel);

        setContentWidget(mainPanel);
    }

    private void bindUIHandlers() {
        btnOk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                okPressed();
            }
        });

        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                chooseClass(null);
            }
        });

        setWindowHiddenHandler(new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                classChosenHandler.onClassChosen(chosenClass);
            }
        });
    }

    private void okPressed() {
        GObjectClass selectedClass = classPanel.getSelectedClass();
        if (selectedClass != null) {
            chooseClass(selectedClass);
        }
    }

    private void chooseClass(GObjectClass chosenClass) {
        if (chosenClass == null || !concreate || chosenClass.concreate) {
            this.chosenClass = chosenClass;
            hide();
        }
    }

    public static GResizableClassDialog showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        GResizableClassDialog classDlg = new GResizableClassDialog(baseClass, defaultClass, concreate, classChosenHandler);
        classDlg.center();
        return classDlg;
    }
}
