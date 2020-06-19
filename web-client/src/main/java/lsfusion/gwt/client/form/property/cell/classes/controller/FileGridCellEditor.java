package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.GImage;
import lsfusion.gwt.client.base.view.ProgressBar;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;
import org.moxieapps.gwt.uploader.client.events.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class FileGridCellEditor extends DialogBasedGridCellEditor {
    private boolean multiple;
    private boolean storeName;
    private String description;
    private ArrayList<String> validContentTypes; // null if FILE (with any extension/contenttype)

    private FocusPanel focusPanel;
    private Button okButton;

    private Uploader newVersionUploader, addUploader;
    private LinkedHashMap<String, String> filePrefixes;
    private HashMap<String, String> fileNames;

    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    public FileGridCellEditor(EditManager editManager, GPropertyDraw property, String description, boolean multiple, boolean storeName, ArrayList<String> validContentTypes) {
        super(editManager, property, messages.fileEditorTitle(), 500, 150);
        this.multiple = multiple;
        this.storeName = storeName;
        this.validContentTypes = validContentTypes;
        this.description = description;
    }

    @Override
    protected Widget createComponent(Element parent, Object oldValue) {
        int addButtonWidth = 133;
        int addButtonHeight = 31;
        
        final ScrollPanel scrollBarPanel = new ScrollPanel();
        final VerticalPanel progressBarPanel = new VerticalPanel();
        progressBarPanel.setWidth("100%");
        scrollBarPanel.add(progressBarPanel);        
        scrollBarPanel.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
        scrollBarPanel.setHeight("100%");
        final LinkedHashMap<String, FileUploadStatusPanel> fileStatusPanels = new LinkedHashMap<>();
        filePrefixes = new LinkedHashMap<>();
        fileNames = new HashMap<>();

        newVersionUploader = new Uploader();

        newVersionUploader.setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile")
                .setButtonText("<button type=\"button\" class=\"gwt-Button\" style=\"height: 27px; width: 129px;\">" + 
                        messages.fileEditorChooseFile() + (multiple ? messages.fileEditorChooseFileSuffix() : "") + "</button>")
                .setButtonWidth(addButtonWidth)
                .setButtonHeight(addButtonHeight)
                .setButtonCursor(Uploader.Cursor.HAND)
                .setButtonAction(multiple ? Uploader.ButtonAction.SELECT_FILES : Uploader.ButtonAction.SELECT_FILE)
                .setFileQueuedHandler(new FileQueuedHandler() {
                    public boolean onFileQueued(final FileQueuedEvent fileQueuedEvent) {
                        if (multiple || fileStatusPanels.size() == 0) {
                            final File file = fileQueuedEvent.getFile();
                            final FileUploadStatusPanel fileStatusPanel = new FileUploadStatusPanel(file.getName());
                            fileStatusPanel.setWidth("100%");
                            fileStatusPanel.getCancelButton().addClickHandler(new ClickHandler() {
                                public void onClick(ClickEvent event) {
                                    newVersionUploader.cancelUpload(file.getId(), false);
                                    fileStatusPanels.remove(file.getId());
                                    filePrefixes.remove(file.getId());
                                    fileNames.remove(file.getId());
                                    progressBarPanel.remove(fileStatusPanel);
                                    if (filePrefixes.isEmpty()) {
                                        okButton.setEnabled(false);
                                    }
                                }
                            });
                            fileStatusPanels.put(file.getId(), fileStatusPanel);

                            String filePrefix = GwtSharedUtils.randomString(15);
                            filePrefixes.put(file.getId(), filePrefix);
                            fileNames.put(file.getId(), file.getName());

                            progressBarPanel.add(fileStatusPanel);
                        }
                        return true;
                    }
                })
                .setUploadProgressHandler(new UploadProgressHandler() {
                    public boolean onUploadProgress(UploadProgressEvent uploadProgressEvent) {
                        fileStatusPanels.get(uploadProgressEvent.getFile().getId()).setProgress(
                                (double) uploadProgressEvent.getBytesComplete() / uploadProgressEvent.getBytesTotal()
                        );
                        return true;
                    }
                })
                .setUploadStartHandler(new UploadStartHandler() {
                    @Override
                    public boolean onUploadStart(UploadStartEvent uploadStartEvent) {
                        newVersionUploader.setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile?sid=" + filePrefixes.get(uploadStartEvent.getFile().getId()));
                        return true;
                    }
                })
                .setUploadCompleteHandler(new UploadCompleteHandler() {
                    public boolean onUploadComplete(UploadCompleteEvent uploadCompleteEvent) {
                        setDisabled(newVersionUploader.getStats().getFilesQueued() > 0);
                        newVersionUploader.startUpload();
                        return true;
                    }
                })
                .setFileDialogStartHandler(new FileDialogStartHandler() {
                    public boolean onFileDialogStartEvent(FileDialogStartEvent fileDialogStartEvent) {
                        if (newVersionUploader.getStats().getUploadsInProgress() <= 0) {
                            progressBarPanel.clear();
                            fileStatusPanels.clear();
                            filePrefixes.clear();
                            fileNames.clear();
                        }
                        return true;
                    }
                })
                .setFileDialogCompleteHandler(new FileDialogCompleteHandler() {
                    public boolean onFileDialogComplete(FileDialogCompleteEvent fileDialogCompleteEvent) {
                        if (fileDialogCompleteEvent.getTotalFilesInQueue() > 0) {
                            if (newVersionUploader.getStats().getUploadsInProgress() <= 0) {
                                newVersionUploader.startUpload();
                            }
                        }
                        return true;
                    }
                })
                .setUploadErrorHandler(new UploadErrorHandler() {
                    public boolean onUploadError(UploadErrorEvent uploadErrorEvent) {
                        setDisabled(newVersionUploader.getStats().getUploadsInProgress() == 1);
                        Window.alert("Upload of file " + uploadErrorEvent.getFile().getName() + " failed due to [" +
                                uploadErrorEvent.getErrorCode().toString() + "]: " + uploadErrorEvent.getMessage()
                        );
                        return true;
                    }
                });

        addUploader  = new Uploader();

        if (multiple) {
            addUploader.setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile")
                    .setButtonText("<button type=\"button\" class=\"gwt-Button\" style=\"height: 27px; width: 129px\">" + messages.fileEditorAddFiles() + "</button>")
                    .setButtonWidth(addButtonWidth)
                    .setButtonHeight(addButtonHeight)
                    .setButtonCursor(Uploader.Cursor.HAND)
                    .setButtonAction(Uploader.ButtonAction.SELECT_FILES)
                    .setFileQueuedHandler(new FileQueuedHandler() {
                        @Override
                        public boolean onFileQueued(FileQueuedEvent fileQueuedEvent) {
                            JsArray array = JsArray.createArray().cast();
                            array.push(fileQueuedEvent.getFile());
                            newVersionUploader.addFilesToQueue(array);
                            return true;
                        }
                    });
        }

        FileQueueErrorHandler fileQueueErrorHandler = new FileQueueErrorHandler() {
            public boolean onFileQueueError(FileQueueErrorEvent fileQueueErrorEvent) {
                setDisabled(newVersionUploader.getStats().getUploadsInProgress() == 1);
                Window.alert("Upload of file " + fileQueueErrorEvent.getFile().getName() + " failed due to [" +
                        fileQueueErrorEvent.getErrorCode().toString() + "]: " + fileQueueErrorEvent.getMessage()
                );
                return true;
            }
        };
        newVersionUploader.setFileQueueErrorHandler(fileQueueErrorHandler);
        if (multiple) {
            addUploader.setFileQueueErrorHandler(fileQueueErrorHandler);
        }

        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.add(newVersionUploader);
        if (multiple) {
            verticalPanel.add(addUploader);
        }

        if (Uploader.isAjaxUploadWithProgressEventsSupported()) {
            final Label dropFilesLabel = new Label(messages.fileEditorDropFiles());
            dropFilesLabel.setStyleName("dropFilesLabel");
            dropFilesLabel.addDragOverHandler(new DragOverHandler() {
                public void onDragOver(DragOverEvent event) {
                    if (!newVersionUploader.getButtonDisabled()) {
                        dropFilesLabel.addStyleName("dropFilesLabelHover");
                    }
                }
            });
            dropFilesLabel.addDragLeaveHandler(new DragLeaveHandler() {
                public void onDragLeave(DragLeaveEvent event) {
                    dropFilesLabel.removeStyleName("dropFilesLabelHover");
                }
            });
            dropFilesLabel.addDropHandler(new DropHandler() {
                public void onDrop(DropEvent event) {
                    dropFilesLabel.removeStyleName("dropFilesLabelHover");

                    if (!multiple && newVersionUploader.getStats().getUploadsInProgress() <= 0) {
                        progressBarPanel.clear();
                        fileStatusPanels.clear();
                        filePrefixes.clear();
                        fileNames.clear();
                    }

                    JsArray droppedFiles = Uploader.getDroppedFiles(event.getNativeEvent());
                    JsArray validFiles = JsArray.createArray().cast();
                    for (int i = 0; i < droppedFiles.length(); i++) {
                        File file = droppedFiles.get(i).cast();
                        if ((validContentTypes == null || validContentTypes.contains(file.getType())) && (multiple || validFiles.length() == 0)) {
                            validFiles.push(file);
                        }
                    }
                    newVersionUploader.addFilesToQueue(validFiles);

                    event.preventDefault();
                    event.stopPropagation();
                }
            });
            verticalPanel.add(dropFilesLabel);
        }

        HorizontalPanel horizontalPanel = new HorizontalPanel();
        horizontalPanel.setWidth("100%");
        horizontalPanel.setHeight("100%");
        horizontalPanel.add(verticalPanel);
        horizontalPanel.setCellWidth(verticalPanel, addButtonWidth + "px");
        Widget horizontalStrut = GwtClientUtils.createHorizontalStrut(5);
        horizontalPanel.add(horizontalStrut);
        horizontalPanel.setCellWidth(horizontalStrut, "5px");
        horizontalPanel.add(scrollBarPanel);
        horizontalPanel.setCellWidth(progressBarPanel, "100%");
        horizontalPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        Button cancelButton = new Button(messages.fileEditorCancel());
        cancelButton.setWidth("70px");
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                cancelEditing();
            }
        });

        okButton = new Button("OK");
        okButton.setWidth("70px");
        okButton.setEnabled(false);
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ArrayList<String> fileSIDS = new ArrayList<>();
                for (String id : filePrefixes.keySet()) {
                    fileSIDS.add(filePrefixes.get(id) + "_" + fileNames.get(id));
                }
                commitEditing(new GFilesDTO(fileSIDS, multiple, storeName, validContentTypes == null));
            }
        });

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(3);
        buttons.add(okButton);
        buttons.add(cancelButton);

        VerticalPanel panel = new VerticalPanel();
        panel.add(horizontalPanel);
        Widget verticalStrut = GwtClientUtils.createVerticalStrut(5);
        panel.add(verticalStrut);
        panel.setCellHeight(verticalStrut, "5px");
        panel.add(buttons);
        panel.setCellWidth(horizontalPanel, "100%");
        panel.setCellHeight(buttons, "24px");
        panel.setCellHorizontalAlignment(buttons, HasAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(buttons, HasVerticalAlignment.ALIGN_BOTTOM);

        String validFileTypes = null;
        if (validContentTypes != null) {
            validFileTypes = "";
            int count = 0;
            for (String extension : validContentTypes) {
                validFileTypes += extension;
                count++;
                if (count < validContentTypes.size()) {
                    validFileTypes += ",";
                }
            }
        }
        if (validFileTypes != null) {
            newVersionUploader.setFileTypes(validFileTypes);
            if (multiple) {
                addUploader.setFileTypes(validFileTypes);
            }
        }
        if (description != null) {
            newVersionUploader.setFileTypesDescription(description);
            if (multiple) {
                addUploader.setFileTypesDescription(description);
            }
        }

        panel.setSize("100%", "100%");

        focusPanel = new FocusPanel(panel);
        focusPanel.addStyleName("uploadDialogContainer");
        focusPanel.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                    cancelEditing();
                }
            }
        });

        focusPanel.addDropHandler(new DropHandler() {
            @Override
            public void onDrop(DropEvent event) {
                // Nothing to do, avoid the default browser behaviour which is to open the file
                event.preventDefault();
                event.stopPropagation();
            }
        });

        focusPanel.addDragOverHandler(new DragOverHandler() {
            public void onDragOver(DragOverEvent event) {
                // Nothing to do, avoid the default browser behaviour which is to open the file
            }
        });

        VerticalPanel vp = new VerticalPanel();
        vp.add(focusPanel);
        vp.setCellHeight(focusPanel, "100%");
        return vp;
    }

    private void setDisabled(boolean disabled) {
        newVersionUploader.setButtonDisabled(disabled);
        addUploader.setButtonDisabled(disabled);
        if (!disabled) {
            if (!filePrefixes.isEmpty()) {
                okButton.setEnabled(true);
            }
            okButton.setFocus(true);
        } else {
            okButton.setEnabled(!disabled);
        }
    }

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        super.startEditing(editEvent, parent, oldValue);
        focusPanel.setFocus(true);
    }

    protected class CancelProgressBarTextFormatter extends ProgressBar.TextFormatter {
        @Override
        protected String getText(ProgressBar bar, double curProgress) {
            if (curProgress < 0) {
                return "Cancelled";
            }
            return ((int) (100 * bar.getPercent())) + "%";
        }
    }

    private class FileUploadStatusPanel extends HorizontalPanel {
        private Label fileNameLabel;
        private ProgressBar progressBar;
        private Image cancelButton = new GImage("delete.png");

        public FileUploadStatusPanel(String fileName) {
            setSpacing(2);
            
            fileNameLabel = new Label(fileName);
            fileNameLabel.setWidth("100%");
            fileNameLabel.addStyleName("upFileNameLabel");

            progressBar = new ProgressBar(0.0, 1.0, 0.0, new CancelProgressBarTextFormatter());
            progressBar.setTitle(fileName);
            progressBar.setWidth("200px");

            cancelButton.addStyleName("displayBlock");

            add(fileNameLabel);
            add(progressBar);
            add(cancelButton);
            setCellWidth(fileNameLabel, "100%");
            setCellVerticalAlignment(fileNameLabel, HasVerticalAlignment.ALIGN_MIDDLE);
            setCellVerticalAlignment(cancelButton, HasVerticalAlignment.ALIGN_MIDDLE);
        }

        public Image getCancelButton() {
            return cancelButton;
        }

        public void setProgress(double progress) {
            progressBar.setProgress(progress);
        }
    }
}
