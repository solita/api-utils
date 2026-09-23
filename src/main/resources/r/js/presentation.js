var presentation = function(presentationInput, olstuffs, layers) {
    var params = new URLSearchParams(window.location.search);
    if (params.get('presentation') == 'diagram') {
        presentationInput.checked = true;
    }
    presentationInput.onclick = function() {
        var params = new URLSearchParams(window.location.search);
        if (presentationInput.checked) {
            params.append('presentation', 'diagram');
        } else {
            params.delete('presentation');
        }
        if (map) {
            olstuffs.refresh(layers());
        }
        var query = params.toString();
        window.history.pushState(null, '', window.location.pathname + (query ? '?' + query : '') + window.location.hash);
    }
};
