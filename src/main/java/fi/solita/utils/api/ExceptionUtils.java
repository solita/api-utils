package fi.solita.utils.api;

import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import fi.solita.utils.functional.Option;

public class ExceptionUtils {

    /**
     * @return Recursively first cause of <i>t</i> which is instance of <i>cause</i>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> Option<T> findCauseFromHierarchy(Throwable t, Class<T> cause) {
        if (cause.isInstance(t)) {
            return Some((T) t);
        } else if (t.getCause() != null) {
            return findCauseFromHierarchy(t.getCause(), cause);
        }
        return None();
    }

}
