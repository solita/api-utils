var search = function(map, searchUrlFunction, searchInput, olstuff, select, unselect) {
    window.onhashchange = function() {
        window.location.hash.split('#').map(function(h) { return decodeURIComponent(h); }).forEach(function(h) {
            let s = searchUrlFunction(h);
            if (s && map.getLayers().getArray().filter(function(x) {return x.getProperties().title == olstuff.mkLayerTitle(h,h);}).length == 0) {
                let layer = olstuff.newVectorLayerNoTile(s, h, h, h);
                layer.setVisible(true);
                map.addLayer(layer);
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
