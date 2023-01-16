document.querySelectorAll('.formats a')
        .forEach(x => x.setAttribute('title', ['html','json','jsonl','geojson','csv','xlsx'].map(y => {
                                                    let ret = x.cloneNode(true);
                                                    ret.innerHTML = y;
                                                    return ret.outerHTML.replace('.html', '.' + y);
                                                }).join(' / ')));