htmx.defineExtension('target-top', {
    onEvent: function(name, evt) {
        if (name === "htmx:beforeRequest" && !evt.detail.elt.hasAttribute('hx-target')) {
            evt.preventDefault();
            let anc = evt.detail.pathInfo.anchor;
            let triggeringEvent = evt.detail.requestConfig.triggeringEvent;
            let targetPath = evt.detail.pathInfo.finalRequestPath + (anc ? '#' + anc : '');
            if (triggeringEvent.ctrlKey || triggeringEvent.metaKey) {
                window.open(targetPath, '_blank');
            } else {
                window.location.assign(targetPath);
            }
            return false;
        }
    }
});