package com.quickbite.order_service.configs;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "trace.id";

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        if (traceId != null) {
            template.header(TRACE_ID_HEADER, traceId);
        }
    }
}
