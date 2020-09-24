package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.GImage;
import lsfusion.gwt.client.base.view.ProgressBar;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;
import org.moxieapps.gwt.uploader.client.events.FileQueueErrorHandler;

import java.util.ArrayList;

public class FileCellEditor extends DialogBasedCellEditor {
    private boolean storeName;
    private String description;
    private ArrayList<String> validContentTypes; // null if FILE (with any extension/contenttype)

    private FocusPanel focusPanel;

    private Uploader newVersionUploader;
    private FileInfo fileInfo = new FileInfo();

    private boolean cancelAfterStart;

    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    public FileCellEditor(EditManager editManager, GPropertyDraw property, String description, boolean storeName, ArrayList<String> validContentTypes) {
        super(editManager, property, messages.fileEditorTitle(), 250, 75);
        this.storeName = storeName;
        this.validContentTypes = validContentTypes;
        this.description = description;
    }

    @Override
    protected Widget createComponent(Event editEvent, Element parent, Object oldValue) {

        final VerticalPanel progressBarPanel = new VerticalPanel();
        progressBarPanel.setWidth("100%");
        final FileUploadProgress fileUploadProgress = new FileUploadProgress();

        newVersionUploader = new Uploader().setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile")
                .setFileQueuedHandler(fileQueuedEvent -> {
                    if (fileUploadProgress.statusPanel == null) {
                        final File file = fileQueuedEvent.getFile();
                        final FileUploadStatusPanel fileStatusPanel = new FileUploadStatusPanel(file.getName());
                        fileStatusPanel.setWidth("100%");
                        fileUploadProgress.statusPanel = fileStatusPanel;

                        fileInfo.filePrefix = GwtSharedUtils.randomString(15);
                        fileInfo.fileName = file.getName();

                        progressBarPanel.add(fileStatusPanel);
                    }
                    return true;
                })
                .setUploadProgressHandler(uploadProgressEvent -> {
                    fileUploadProgress.statusPanel.setProgress((double) uploadProgressEvent.getBytesComplete() / uploadProgressEvent.getBytesTotal());
                    return true;
                })
                .setUploadStartHandler(uploadStartEvent -> {
                    newVersionUploader.setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile?sid=" + fileInfo.filePrefix);
                    return true;
                })
                .setUploadCompleteHandler(uploadCompleteEvent -> {
                    newVersionUploader.startUpload();
                    return true;
                })
                .setFileDialogStartHandler(fileDialogStartEvent -> {
                    if (newVersionUploader.getStats().getUploadsInProgress() <= 0) {
                        progressBarPanel.clear();
                        fileUploadProgress.clear();
                        fileInfo.clear();
                    }
                    return true;
                })
                .setFileDialogCompleteHandler(fileDialogCompleteEvent -> {
                    if (fileDialogCompleteEvent.getTotalFilesInQueue() > 0) {
                        if (newVersionUploader.getStats().getUploadsInProgress() <= 0) {
                            newVersionUploader.startUpload();
                        }
                    }
                    return true;
                })
                .setUploadSuccessHandler(uploadSuccessEvent -> {
                    if (!fileUploadProgress.success) {
                        commit();
                        fileUploadProgress.success = true;
                    }
                    return true;
                })
                .setUploadErrorHandler(uploadErrorEvent -> {
                    Window.alert("Upload of file " + uploadErrorEvent.getFile().getName() + " failed due to [" + uploadErrorEvent.getErrorCode().toString() + "]: " + uploadErrorEvent.getMessage());
                    return true;
                });

        FileQueueErrorHandler fileQueueErrorHandler = fileQueueErrorEvent -> {
            Window.alert("Upload of file " + fileQueueErrorEvent.getFile().getName() + " failed due to [" + fileQueueErrorEvent.getErrorCode().toString() + "]: " + fileQueueErrorEvent.getMessage());
            return true;
        };
        newVersionUploader.setFileQueueErrorHandler(fileQueueErrorHandler);

        VerticalPanel panel = new VerticalPanel();
        panel.add(progressBarPanel);

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
        }
        if (description != null) {
            newVersionUploader.setFileTypesDescription(description);
        }

        if(GKeyStroke.isDropEvent(editEvent)) {

            JsArray droppedFiles = Uploader.getDroppedFiles(editEvent);
            cancelAfterStart = !addFilesToUploader(droppedFiles);

        } else {

            final FileUpload fileUpload = new FileUpload();
            fileUpload.getElement().setAttribute("accept", validFileTypes);

            fileUpload.addChangeHandler(event -> {
                String filename = fileUpload.getFilename();
                if (filename.length() > 0) {
                    JsArray selectedFiles = nativeGetSelectedFiles(fileUpload.getElement());
                    if(!addFilesToUploader(selectedFiles))
                        cancelEditing();
                } else {
                    cancelEditing();
                }
            });

            panel.add(fileUpload);

            fileUpload.getElement().<InputElement>cast().click();
        }

        focusPanel = new FocusPanel(panel);
        focusPanel.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                GwtClientUtils.stopPropagation(event);
                cancelEditing();
            }
        });

        return focusPanel;
    }

    @Override
    protected void afterStartEditing() {
        super.afterStartEditing();
        if (cancelAfterStart) {
            cancelEditing();
        }
    }

    private boolean addFilesToUploader(JsArray files) {
        JsArray validFiles = JsArray.createArray().cast();
        for (int i = 0; i < files.length(); i++) {
            File file = files.get(i).cast();
            if ((validContentTypes == null || validContentTypes.contains(file.getType())) && (validFiles.length() == 0)) {
                validFiles.push(file);
            }
        }

        boolean hasValidFiles = validFiles.length() > 0;
        if(hasValidFiles) {
            newVersionUploader.addFilesToQueue(validFiles);
            newVersionUploader.startUpload();
        }
        return hasValidFiles;
    }

    public void commit() {
        ArrayList<String> fileSIDS = new ArrayList<>();
        fileSIDS.add(fileInfo.filePrefix + "_" + fileInfo.fileName);
        commitEditing(new GFilesDTO(fileSIDS, false, storeName, validContentTypes == null));
    }

    private static native JsArray nativeGetSelectedFiles(Element fileInputElement) /*-{
        return fileInputElement.files;
    }-*/;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        super.startEditing(editEvent, parent, oldValue);
        if(focusPanel != null) {
            focusPanel.setFocus(true);
        }
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

    private class FileInfo {
        public boolean dialogStarted;
        public String filePrefix;
        public String fileName;

        public void clear() {
            dialogStarted = false;
            filePrefix = null;
            fileName = null;
        }
    }

    private class FileUploadProgress {
        public FileUploadStatusPanel statusPanel;
        public boolean success; //todo: почему-то success вызывается 2 раза

        public void clear() {
            statusPanel = null;
            success = false;
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
