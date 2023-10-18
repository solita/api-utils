var presentation = function(presentationInput) {
    var params = new URLSearchParams(window.location.search);
    if (params.get('presentation') == 'diagram') {
        presentationInput.checked = true;
    }
    presentationInput.onclick = function() {
        if (presentationInput.checked) {
            params.append('presentation', 'diagram');
        } else {
            params.delete('presentation');
        }
        window.location.search = decodeURIComponent(params);
    }
};
