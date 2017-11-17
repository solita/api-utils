package fi.solita.utils.api.base;

import org.eclipse.persistence.jaxb.DefaultXMLNameTransformer;

public class XmlNameGenerator extends DefaultXMLNameTransformer {
    @Override
    public String transformRootElementName(String className) {
        String ret = super.transformRootElementName(className);
        if (ret.endsWith("Dto")) {
            ret = ret.substring(0, ret.length() - 3);
        }
        return ret;
    }
}