package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GProgressBar;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ProgressBar;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;

public class FileCellEditor implements CellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private EditManager editManager;
    private boolean storeName;
    private String description;
    private ArrayList<String> validContentTypes; // null if FILE (with any extension/contenttype)

    private Uploader newVersionUploader;
    private FileInfo fileInfo = new FileInfo();
    
    public FileCellEditor(EditManager editManager, String description, boolean storeName, ArrayList<String> validContentTypes) {
        this.editManager = editManager;
        this.storeName = storeName;
        this.validContentTypes = validContentTypes;
        this.description = description;
    }

    private boolean addFilesToUploader(JsArray files) {
        JsArray validFiles = JsArray.createArray().cast();
        for (int i = 0; i < files.length(); i++) {
            File file = files.get(i).cast();
            String type = file.getType(); //some input files may have empty type
            if ((validContentTypes == null || emptyValidContentType() || isRedundantString(type) || validContentTypes.contains(type)) && (validFiles.length() == 0)) {
                validFiles.push(file);
            }
        }

        boolean hasValidFiles = validFiles.length() > 0;
        if(hasValidFiles) {
            newVersionUploader = createUploader();
            newVersionUploader.addFilesToQueue(validFiles);
            newVersionUploader.startUpload();
        }
        return hasValidFiles;
    }

    private boolean emptyValidContentType() {
        return validContentTypes.size() == 1 && validContentTypes.get(0).isEmpty();
    }

    public void commit() {
        ArrayList<String> fileSIDS = new ArrayList<>();
        fileSIDS.add(fileInfo.filePrefix + "_" + fileInfo.fileName);
        editManager.commitEditing(new GFilesDTO(fileSIDS, false, storeName, validContentTypes == null));
        newVersionUploader = null;
    }

    public void cancel() {
        if(newVersionUploader != null) {
            newVersionUploader.cancelUpload();
        }
        editManager.cancelEditing();
        newVersionUploader = null;
    }

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        if(GKeyStroke.isDropEvent(editEvent)) {
            drop(editEvent);
        } else {
            click(parent, createFileInputElement());
        }
    }

    private void drop(Event editEvent) {
        JsArray droppedFiles = Uploader.getDroppedFiles(editEvent);
        if(!addFilesToUploader(droppedFiles))
            cancel();
    }

    private native void click(Element parent, Element inputElement) /*-{
        var instance = this;

        var needToCancel = true;
        inputElement.onchange = function () {
            needToCancel = !instance.@FileCellEditor::addFilesToUploader(*)(this.files);
        }

        parent.onfocus = function () {
            setTimeout(function () {//onfocus event fires before onchange event, so we need a timeout
                if (needToCancel) {
                    instance.@FileCellEditor::cancel(*)();
                    needToCancel = false;
                }
                parent.onfocus = null;
            }, 300)
        }

        inputElement.click();
    }-*/;

    private InputElement createFileInputElement() {
        InputElement inputElement = Document.get().createFileInputElement();
        if(validContentTypes != null) {
            String accept = "";
            for(String type : validContentTypes) {
                accept += (accept.isEmpty() ? "" : ",") + type;
            }
            inputElement.setAccept(accept);
        }
        return inputElement;
    }

    private Uploader createUploader() {
        LoadingBox loadingBox = new LoadingBox(this::cancel);

        Uploader newVersionUploader = new Uploader();
        newVersionUploader.setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile")
                .setFileQueuedHandler(fileQueuedEvent -> {
                    final File file = fileQueuedEvent.getFile();
                    fileInfo.filePrefix = GwtSharedUtils.randomString(15);
                    fileInfo.fileName = file.getName();
                    loadingBox.showLoadingBox();
                    return true;
                })
                .setUploadProgressHandler(uploadProgressEvent -> {
                    loadingBox.setProgress(new GProgressBar(fileInfo.fileName, (int) uploadProgressEvent.getBytesComplete(), (int) uploadProgressEvent.getBytesTotal()));
                    return true;
                })
                .setUploadStartHandler(uploadStartEvent -> {
                    newVersionUploader.setUploadURL(GwtClientUtils.getWebAppBaseURL() + "uploadFile?sid=" + fileInfo.filePrefix);
                    return true;
                })
                .setUploadSuccessHandler(uploadSuccessEvent -> {
                    loadingBox.hideLoadingBox();
                    commit();
                    return true;
                });

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

        return newVersionUploader;
    }

    private class FileInfo {
        public boolean dialogStarted;
        public String filePrefix;
        public String fileName;
    }

    private class LoadingBox extends WindowBox {
        private VerticalPanel progressPanel;
        private GProgressBar prevProgress;
        private Timer timer;

        public LoadingBox(Runnable cancelAction) {
            super(false, false, false);
            setModal(true);
            setGlassEnabled(true);

            setText(messages.loading());

            VerticalPanel mainPanel = new VerticalPanel();

            progressPanel = new VerticalPanel();
            mainPanel.add(progressPanel);

            HorizontalPanel bottomPanel = new HorizontalPanel();
            bottomPanel.setWidth("100%");
            bottomPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

            Button btnCancel = new Button(messages.cancel());
            btnCancel.addClickHandler(clickEvent -> {
                hideLoadingBox();
                cancelAction.run();
            });
            bottomPanel.add(btnCancel);

            mainPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
            mainPanel.add(bottomPanel);

            setWidget(mainPanel);
        }

        public void makeMaskVisible(boolean visible) {
            getElement().getStyle().setOpacity(visible ? 1 : 0);
            getGlassElement().getStyle().setOpacity(visible ? 0.3 : 0);
        }

        public void showLoadingBox() {
            timer = new Timer() {
                @Override
                public void run() {
                    show();
                }
            };
            timer.schedule(1000);
        }

        public void hideLoadingBox() {
            if(isShowing())
                hide();
            progressPanel.clear();
            timer.cancel();
        }

        public void setProgress(GProgressBar progress) {
            if (prevProgress == null || !prevProgress.equals(progress)) {
                progressPanel.clear();
                progressPanel.add(createProgressBarPanel(progress));
                prevProgress = progress;
            }

            if(!isShowing())
                center();
        }

        private FlexPanel createProgressBarPanel(final GProgressBar line) {
            FlexPanel progressBarPanel = new FlexPanel(true);
            progressBarPanel.addStyleName("stackMessage");
            ProgressBar progressBar = new ProgressBar(0, line.total, line.progress, new ProgressBar.TextFormatter() {
                @Override
                protected String getText(ProgressBar bar, double curProgress) {
                    return line.message;
                }
            });
            progressBar.setWidth("200px");
            progressBarPanel.add(progressBar);
            if (line.params != null)
                progressBarPanel.add(new HTML(line.params));
            return progressBarPanel;
        }
    }
}
