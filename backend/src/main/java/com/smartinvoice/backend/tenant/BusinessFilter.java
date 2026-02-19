package com.smartinvoice.backend.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class BusinessFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String businessId = httpRequest.getHeader("X-Business-ID");

        if (businessId != null) {
            BusinessContext.setBusinessId(businessId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            BusinessContext.clear();
        }
    }
}
