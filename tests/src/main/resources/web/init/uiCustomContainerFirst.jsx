// one of the two no-build React views of UiCustomContainer.lsf's form uiCustomContainerTwo (UiCustomContainerIT): every
// .jsx in web/init is turned into a classic script of its own on the server, and the scripts of one page share one
// global scope, so what the transform adds to each must not clash with what it adds to the other
function UiCustomContainerFirst() {
    return <div className="ui-custom-container-first">The first view</div>;
}
