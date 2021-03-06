package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.TimeoutAwareAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.fsm.Yatomata;
import ru.yandex.qatools.fsm.camel.annotations.OnTimeout;
import ru.yandex.qatools.fsm.camel.util.ReflectionUtil.AnnotatedMethodHandler;

import java.lang.reflect.Method;

import static java.lang.String.format;
import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.forEachAnnotatedMethod;

/**
 * @author Innokenty Shuvalov (mailto: innokenty@yandex-team.ru)
 */
public class YatomataTimeoutAwareAggregationStrategy<T>
        extends YatomataAggregationStrategy<T>
        implements TimeoutAwareAggregationStrategy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public YatomataTimeoutAwareAggregationStrategy(Class<T> fsmClass) {
        super(fsmClass);
    }

    @Override
    public void timeout(Exchange oldExchange, final int index, final int total, final long timeout) {
        try {
            final Yatomata<T> fsmEngine = buildFsmEngine(oldExchange, oldExchange);
            forEachAnnotatedMethod(getFsmClass(), OnTimeout.class, new AnnotatedMethodHandler<OnTimeout>() {
                @Override
                public void handle(Method method, OnTimeout annotation) {
                    try {
                        method.invoke(fsmEngine.getFSM(),
                                fsmEngine.getCurrentState(), index, total, timeout);
                    } catch (Exception e) {
                        logger.error(format("Failed to invoke method '%s' on FSM %s!",
                                method.getName(), fsmEngine.getFSM()), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error(format("Failed to invoke @OnTimeout method on FSM %s!",
                    getFsmClass()), e);
        }
    }
}
