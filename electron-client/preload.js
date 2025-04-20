const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    readFile: (filePath) => ipcRenderer.invoke('read-file', { filePath }),
    writeFile: (filePath, content) => ipcRenderer.invoke('write-file', { filePath, content }),
    runCommand: (command) => ipcRenderer.invoke('run-command', command)
});
