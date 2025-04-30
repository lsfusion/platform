const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    readFile: (filePath) => ipcRenderer.invoke('read-file', { filePath }),
    deleteFile: (filePath) => ipcRenderer.invoke('delete-file', filePath),
    fileExists: (filePath) => ipcRenderer.invoke('file-exists', filePath),
    writeFile: (filePath, content) => ipcRenderer.invoke('write-file', { filePath, content }),
    runCommand: (command) => ipcRenderer.invoke('run-command', command)
});
