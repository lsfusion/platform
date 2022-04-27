package lsfusion.gwt.client.form.classes.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.classes.GObjectClass;

public class GClassDialog extends WindowBox {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private ClassTreePanel classPanel;

    private Button btnOk;
    private Button btnCancel;

    private GObjectClass chosenClass;

    public GClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        super(false, false, true);

        this.concreate = concreate;
        this.classChosenHandler = classChosenHandler;

        setModal(true);
        setGlassEnabled(true);
        setText(messages.choosingClass());

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

        setWidget(mainPanel);
        center();
    }

    private void bindUIHandlers() {
        btnOk.addClickHandler(event -> okPressed());
        btnCancel.addClickHandler(event -> chooseClass(null));
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

    public static void showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        new GClassDialog(baseClass, defaultClass, concreate, classChosenHandler).show();
    }

    @Override
    public void hide(boolean autoClosed) {
        super.hide(autoClosed);
        classChosenHandler.onClassChosen(chosenClass);
    }
}
