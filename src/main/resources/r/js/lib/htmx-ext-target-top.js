htmx.defineExtension('target-top', {
    onEvent: function(name, evt) {
        if (name === "htmx:beforeRequest") {
            evt.preventDefault();
            let anc = evt.detail.pathInfo.anchor;
            window.location.assign(evt.detail.pathInfo.finalRequestPath + (anc ? '#' + anc : ''));
        }
    }
});