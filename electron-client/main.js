const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');

function createWindow() {
    const win = new BrowserWindow({
        width: 1000,
        height: 700,
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
        },
        icon: path.join(__dirname, 'logo.png')
    });

    win.setMenuBarVisibility(false);
    win.loadURL('http://127.0.0.1:8888/main');
}

app.whenReady().then(createWindow);

ipcMain.handle('save-file', async (event, { filePath, content }) => {
    try {
        const buffer = Buffer.from(content, 'base64');
        fs.writeFileSync(filePath, buffer);
        return { success: true };
    } catch (err) {
        return { error: err.message };
    }
});