package fi.solita.utils.api;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

public class CompositeResultMatcher implements ResultMatcher {

    private final ResultMatcher[] matchers;
    
    public static final ResultMatcher of(ResultMatcher... matchers) {
        return new CompositeResultMatcher(matchers);
    }

    private CompositeResultMatcher(ResultMatcher... matchers) {
        this.matchers = matchers;
    }
    
    @Override
    public void match(MvcResult result) throws Exception {
        for (ResultMatcher rm: matchers) {
            rm.match(result);
        }
    }

}
