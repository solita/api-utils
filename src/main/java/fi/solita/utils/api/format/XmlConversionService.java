package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableMap;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.flatMap;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.jaxb.metadata.MetadataSource;

import fi.solita.utils.api.base.xml.XmlModule;
import fi.solita.utils.api.format.xml.Dummy;

public class XmlConversionService {
    
    private final Marshaller marshaller;

    public XmlConversionService(XmlModule xmlModule, Iterable<XmlAdapter<?,?>> adapters, @SuppressWarnings("rawtypes") Class... rootClasses) {
        for (Class<?> rootClass: rootClasses) {
            if (!rootClass.isAnnotationPresent(XmlRootElement.class)) {
                throw new IllegalStateException("Missing @XmlRootElement: " + rootClass.getName());
            }
        }
        try {
            Map<String, MetadataSource> m = newMutableMap();
            for (Class<?> clazz: rootClasses) {
                m.put(clazz.getPackage().getName(), xmlModule.getMetadataSource());
            }
            for (Package pkg: xmlModule.getAdditionalPackages()) {
                m.put(pkg.getName(), xmlModule.getMetadataSource());
            }

            Map<String, Object> properties = newMutableMap();
            properties.put(JAXBContextProperties.OXM_METADATA_SOURCE, m);
            
            // include .xml package to pull in jaxb.properties.
            // include XmlRegistry
            // include all nested classes of XmlAdapters
            Iterable<Class<?>> additionalRoots = concat(newList(xmlModule.getXmlRegistry(), Dummy.class), flatMap(XmlConversionService_.getNestedClasses, adapters));
            JAXBContext context = JAXBContext.newInstance(newArray(Class.class, concat(additionalRoots, rootClasses)), properties);
            
            this.marshaller = context.createMarshaller();
            
            // Gotta instantiate adapters manually to get versioned Serializers in.
            for (XmlAdapter<?, ?> adapter: adapters) {
                marshaller.setAdapter(adapter);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static Iterable<Class<?>> getNestedClasses(Object o) {
        return newList(o.getClass().getDeclaredClasses());
    }

    public byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            marshaller.marshal(obj, os);
            return os.toByteArray();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
