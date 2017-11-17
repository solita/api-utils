package fi.solita.utils.api.types;

public interface TransformedPoint {
    public String getSourceCRS();
    
    public String getTargetCRS();
    
    public double getX();
    
    public double getY();
}