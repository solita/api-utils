package fi.solita.utils.api.base.xml;

import java.util.Map;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.eclipse.persistence.jaxb.metadata.MetadataSourceAdapter;
import org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType;
import org.eclipse.persistence.jaxb.xmlmodel.XmlBindings;
import org.eclipse.persistence.jaxb.xmlmodel.XmlJavaTypeAdapter;
import org.eclipse.persistence.jaxb.xmlmodel.XmlJavaTypeAdapters;

@SuppressWarnings("rawtypes")
public class XmlMetadataSource extends MetadataSourceAdapter {
    private final Class<? extends XmlNameGenerator> nameTransformer;
    private final Map<? extends Class<?>, XmlAdapter<?, ?>> adapters;

    public XmlMetadataSource(Class<? extends XmlNameGenerator> nameTransformer, Map<? extends Class<?>, XmlAdapter<?,?>> adapters) {
        this.nameTransformer = nameTransformer;
        this.adapters = adapters;
    }

    public XmlBindings getXmlBindings(java.util.Map<String,?> properties, ClassLoader classLoader) {
        XmlBindings xmlBindings = new XmlBindings();
        xmlBindings.setXmlAccessorType(XmlAccessType.FIELD);
        xmlBindings.setXmlNameTransformer(nameTransformer.getName());
        
        XmlJavaTypeAdapters xmlJavaTypeAdapters = new XmlJavaTypeAdapters();
        xmlBindings.setXmlJavaTypeAdapters(xmlJavaTypeAdapters);
        for (Map.Entry<? extends Class, XmlAdapter<?,?>> adapter: adapters.entrySet()) {
            XmlJavaTypeAdapter xmlJavaTypeAdapter = new XmlJavaTypeAdapter();
            xmlJavaTypeAdapter.setType(adapter.getKey().getName());
            xmlJavaTypeAdapter.setValue(adapter.getValue().getClass().getName());
            xmlJavaTypeAdapters.getXmlJavaTypeAdapter().add(xmlJavaTypeAdapter);
        }
        
        return xmlBindings;
    }
}