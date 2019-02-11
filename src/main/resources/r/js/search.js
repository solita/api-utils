var search = function(map, searchUrlFunction, searchInput, olstuff) {
    window.onhashchange = function() {
        let s = searchUrlFunction(window.location.hash);
        if (s) {
            let layer = olstuff.newVectorLayerNoTile(s, 'Haku', 'Search');
            layer.setVisible(true);
            layer.setMap(map);
            layer.on('change', function(e) {
                map.getView().fit(layer.getSource().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
            });
        }
    }
    
    window.onhashchange();
    
    if (window.location.hash) {
        searchInput.value = window.location.hash.substring(1);
    }
    
    var searchChanged = function() {
        window.location.hash = '#' + searchInput.value;
    };
    
    searchInput.onchange = searchChanged;
    
    return searchChanged;
};
