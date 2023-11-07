package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GProgressBar;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;
import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;
import org.moxieapps.gwt.uploader.client.events.UploadErrorEvent;

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

        // we can catch "File name too long" error only in UploadFileRequestHandler,
        // but we need to handle it in this place,
        // we use a hack: in UploadFileRequestHandler write 270 code in response when this error occurs,
        // and in this place when 270 code (FILE_VALIDATION_FAILED) appears we know that it is an error related to "File name too long".
        newVersionUploader.setUploadErrorHandler(uploadErrorEvent -> {
            if (!uploadErrorEvent.getErrorCode().equals(UploadErrorEvent.ErrorCode.FILE_CANCELLED)) { // this check is necessary because loadingBox.hideLoadingBox() throws FILE_CANCELLED error
                String errorMessage = uploadErrorEvent.getMessage().endsWith(String.valueOf(-UploadErrorEvent.ErrorCode.FILE_VALIDATION_FAILED.toInt())) ?
                        ClientMessages.Instance.get().fileNameTooLong() : uploadErrorEvent.getMessage();
                loadingBox.hideLoadingBox(true);
                throw new RuntimeException(errorMessage);
            }
            return false;
        });
        return hasValidFiles;
    }

    private boolean emptyValidExtension() {
        return validExtensions.size() == 1 && validExtensions.get(0).isEmpty();
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if(cancel && newVersionUploader != null)
            newVersionUploader.cancelUpload();
        newVersionUploader = null;
    }

    private boolean uploaded = false;

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        if(!uploaded)
            throw new InvalidEditException();
        return PValue.getPValue(new GFilesDTO(fileInfo.filePrefix + "_" + fileInfo.fileName, fileInfo.fileName, storeName, validExtensions == null, named));
    }

    @Override
    public void start(EventHandler handler, Element parent, PValue oldValue) {
        Event event;
        if(handler != null && GKeyStroke.isDropEvent(event = handler.event)) {
            drop(event, parent);
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
    private LoadingBox loadingBox;
    private Uploader createUploader(Element parent) {
        loadingBox = new LoadingBox(() -> cancel(parent));

        Uploader newVersionUploader = new Uploader();
        newVersionUploader.setUploadURL(GwtClientUtils.getUploadURL(null)) // not sure that is needed
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
                    newVersionUploader.setUploadURL(GwtClientUtils.getUploadURL(fileInfo.filePrefix));
                    return true;
                })
                .setUploadSuccessHandler(uploadSuccessEvent -> {
                    loadingBox.hideLoadingBox(false);
                    uploaded = true;
                    commit(parent, CommitReason.FORCED);
                    return true;
                });

        return newVersionUploader;
    }

    private class FileInfo {
        public String filePrefix;
        public String fileName;
    }

    private class LoadingBox extends DialogModalWindow {

        private final SimplePanel progressPane;
        private GProgressBar prevProgress;
        private Timer timer;
        private final Runnable cancelAction;

        public LoadingBox(Runnable cancelAction) {
            super(messages.loading(), false, ModalWindowSize.FIT_CONTENT);
            this.cancelAction = cancelAction;

            progressPane = new SimplePanel();
            progressPane.setStyleName("dialog-loading-progress");
            setBodyWidget(progressPane);

            addFooterWidget(new FormButton(messages.cancel(), FormButton.ButtonStyle.SECONDARY, clickEvent -> hideLoadingBox(true)));
        }

        private boolean isShowing = false;
        public void showLoadingBox() {
            timer = new Timer() {
                @Override
                public void run() {
                    isShowing = true; show();
                }
            };
            timer.schedule(1000);
        }

        public void hideLoadingBox(boolean cancel) {
            if(isShowing) {
                isShowing = false;
                hide();
            }
            timer.cancel();

            if (cancel)
                cancelAction.run();
        }

        public void setProgress(GProgressBar progress) {
            if (prevProgress == null || !prevProgress.equals(progress)) {
                progressPane.setWidget(new ProgressBar(0, progress.total, progress.progress, new ProgressBar.TextFormatter() {
                    @Override
                    protected String getText(ProgressBar bar, double curProgress) {
                        return progress.message;
                    }
                }));
                prevProgress = progress;
            }
        }
    }
}
