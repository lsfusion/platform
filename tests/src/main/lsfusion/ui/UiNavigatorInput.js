window.uiNavigatorInputThroughController = function (controller, resolve, reject) {
    controller.exec('UiNavigatorInput.uiNavigatorInputAsk').then(resolve, reject);
};
