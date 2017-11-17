package fi.solita.utils.api.base;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public abstract class XmlAdapterBase<S extends Serializers,T1,T2> extends XmlAdapter<T1,T2> {
    // FIXME: meta-luokkien generointi bugaa, joten pitää kierrättää protected-metodin kautta
    private S s;
    protected S s() {
        return s;
    }
    public XmlAdapter<T1,T2> with(S s) {
        this.s = s;
        return this;
    }
}