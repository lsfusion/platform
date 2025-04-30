const { app, BrowserWindow} = require('electron');
const path = require('path');

function createWindow() {
    const win = new BrowserWindow({
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            preload: path.join(__dirname, 'preload.js'),
        },
        icon: path.join(__dirname, 'logo.png')
    });
    win.maximize();
    win.setMenuBarVisibility(false);
    win.loadURL('http://127.0.0.1:8888/main');
}

app.whenReady().then(createWindow);