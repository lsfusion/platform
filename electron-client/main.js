const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const { exec } = require('child_process');

function createWindow() {
    const win = new BrowserWindow({
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
        },
        icon: path.join(__dirname, 'logo.png')
    });
    win.maximize();
    win.setMenuBarVisibility(false);
    win.loadURL('http://127.0.0.1:8888/main');
}

app.whenReady().then(createWindow);

ipcMain.handle('read-file', async (event, { filePath }) => {
    try {
        const buffer = fs.readFileSync(filePath);
        const base64 = buffer.toString('base64');
        return { content: base64 };
    } catch (err) {
        return { error: err.message };
    }
});

ipcMain.handle('write-file', async (event, { filePath, content }) => {
    try {
        const buffer = Buffer.from(content, 'base64');
        fs.writeFileSync(filePath, buffer);
        return { success: true };
    } catch (err) {
        return { error: err.message };
    }
});

ipcMain.handle('run-command', async (event, command) => {
    return new Promise((resolve) => {
        exec(command, (error, stdout, stderr) => {
            resolve({
                output: stdout,
                error: stderr,
                exitValue: error ? error.code : 0
            });
        });
    });
});