package fi.solita.utils.api.base;

import java.util.Collection;

import org.eclipse.persistence.jaxb.metadata.MetadataSource;

public interface XmlModule {
    MetadataSource getMetadataSource();
    Collection<Package> getAdditionalPackages();
    Class<?> getXmlRegistry();
}
