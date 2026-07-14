package com.adren.travel.security.internal;

import com.adren.travel.shared.TraceIds;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void generatesATraceIdWhenNoneIsSuppliedAndEchoesItOnTheResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIds.HEADER)).isNotBlank();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void reusesAnIncomingTraceIdHeaderRatherThanGeneratingANewOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.HEADER, "client-supplied-trace-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIds.HEADER)).isEqualTo("client-supplied-trace-id");
    }

    @Test
    void putsTheTraceIdInMdcForTheDurationOfTheRequestOnly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.HEADER, "trace-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicMdcCapture capture = new AtomicMdcCapture();
        FilterChain chain = (req, res) -> capture.value = MDC.get(TraceIds.MDC_KEY);

        filter.doFilter(request, response, chain);

        assertThat(capture.value).isEqualTo("trace-abc");
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    private static class AtomicMdcCapture {
        volatile String value;
    }
}
