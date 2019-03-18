var search = function(map, searchUrlFunction, searchInput, olstuff, select, unselect) {
    window.onhashchange = function() {
        window.location.hash.split('#').forEach(function(h) {
            let s = searchUrlFunction(h);
            if (s) {
                let layer = olstuff.newVectorLayerNoTile(s, 'Haku', 'Search');
                layer.setVisible(true);
                layer.setMap(map);
                layer.on('change', function(e) {
                    map.getView().fit(layer.getSource().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
                });
                hover(map, [layer], select, unselect);
            }
        });
    }
    
    window.onhashchange();
    
    if (window.location.hash) {
        searchInput.value = decodeURI(window.location.hash.substring(1));
    }
    
    var searchChanged = function() {
        window.location.hash = '#' + encodeURI(searchInput.value);
    };
    
    searchInput.onchange = searchChanged;
    
    return searchChanged;
};
