package fi.solita.utils.api.util;




import java.util.Optional;

public class ExceptionUtils {

    /**
     * @return Recursively first cause of <i>t</i> which is instance of <i>cause</i>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> Optional<T> findCauseFromHierarchy(Throwable t, Class<T> cause) {
        if (cause.isInstance(t)) {
            return Optional.of((T) t);
        } else if (t.getCause() != null) {
            return findCauseFromHierarchy(t.getCause(), cause);
        }
        return Optional.empty();
    }

}
