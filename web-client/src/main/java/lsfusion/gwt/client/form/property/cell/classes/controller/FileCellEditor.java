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
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;
import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;

import java.util.List;

public class FileCellEditor extends ARequestValueCellEditor implements KeepCellEditor {

    private static final ClientMessages messages = ClientMessages.Instance.get();
    private boolean storeName;
    private List<String> validExtensions; // null if FILE (with any extension/contenttype)
    private boolean named;

    private Uploader newVersionUploader;
    private FileInfo fileInfo = new FileInfo();
    
    public FileCellEditor(EditManager editManager, boolean storeName, List<String> validExtensions, boolean named) {
        super(editManager);
        this.storeName = storeName;
        this.validExtensions = validExtensions;
        this.named = named;
    }

    private boolean addFilesToUploader(JsArray files, Element parent) {
        JsArray validFiles = JsArray.createArray().cast();
        for (int i = 0; i < files.length(); i++) {
            File file = files.get(i).cast();
            String extension = GwtClientUtils.getFileExtension(file.getName()).toLowerCase();
            if ((validExtensions == null || emptyValidExtension() || validExtensions.contains(extension)) && (validFiles.length() == 0)) {
                validFiles.push(file);
            }
        }

        boolean hasValidFiles = validFiles.length() > 0;
        if(hasValidFiles) {
            newVersionUploader = createUploader(parent);
            newVersionUploader.addFilesToQueue(validFiles);
            newVersionUploader.startUpload();
        }
        return hasValidFiles;
    }

    private boolean emptyValidExtension() {
        return validExtensions.size() == 1 && validExtensions.get(0).isEmpty();
    }

    @Override
    public void stop(Element parent, boolean cancel) {
        if(cancel && newVersionUploader != null)
            newVersionUploader.cancelUpload();
        newVersionUploader = null;
    }

    private boolean uploaded = false;

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        if(!uploaded)
            return RequestValueCellEditor.invalid;
        return new GFilesDTO(fileInfo.filePrefix + "_" + fileInfo.fileName, fileInfo.fileName, storeName, validExtensions == null, named);
    }

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        if(GKeyStroke.isDropEvent(editEvent)) {
            drop(editEvent, parent);
        } else {
            click(parent, createFileInputElement());
        }
    }

    private void drop(Event editEvent, Element parent) {
        JsArray droppedFiles = Uploader.getDroppedFiles(editEvent);
        if(!addFilesToUploader(droppedFiles, parent))
            cancel(parent);
    }

    private native void click(Element parent, Element inputElement) /*-{
        var instance = this;

        var needToCancel = true;
        inputElement.onchange = function () {
            needToCancel = !instance.@FileCellEditor::addFilesToUploader(*)(this.files, parent);
        }

        var focusedElement = @GwtClientUtils::getFocusedElement()();
        focusedElement.onfocus = function () {
            setTimeout(function () {//onfocus event fires before onchange event, so we need a timeout
                if (needToCancel) {
                    instance.@FileCellEditor::cancel(Lcom/google/gwt/dom/client/Element;)(parent);
                    needToCancel = false;
                }
                focusedElement.onfocus = null;
            }, 300)
        }

        inputElement.click();
    }-*/;

    private InputElement createFileInputElement() {
        InputElement inputElement = Document.get().createFileInputElement();
        if(validExtensions != null) {
            String accept = "";
            for(String type : validExtensions) {
                accept += (accept.isEmpty() ? "" : ",") + "." + type;
            }
            inputElement.setAccept(accept);
        }
        return inputElement;
    }

    private Uploader createUploader(Element parent) {
        LoadingBox loadingBox = new LoadingBox(() -> cancel(parent));

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
                    uploaded = true;
                    validateAndCommit(parent, true, CommitReason.FORCED);
                    return true;
                });

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
