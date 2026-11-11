package com.bakery.bakeryplatform.config;

import com.bakery.bakeryplatform.service.user.SiteIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminAccessConfig implements WebMvcConfigurer {
    private final SiteIdentity identity;

    public AdminAccessConfig(SiteIdentity identity) {
        this.identity = identity;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAccessInterceptor(identity));
    }

    private static final class AdminAccessInterceptor implements HandlerInterceptor {
        private final SiteIdentity identity;

        private AdminAccessInterceptor(SiteIdentity identity) {
            this.identity = identity;
        }

        @Override
        public boolean preHandle(
                HttpServletRequest request,
                HttpServletResponse response,
                Object handler) throws Exception {
            String path = request.getRequestURI();
            if (!requiresAdmin(path, request.getMethod())) {
                return true;
            }

            HttpSession session = request.getSession(false);
            if (session != null && identity.admin(session)) {
                return true;
            }

            if (path.startsWith("/api/")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Administrator login required.");
            } else {
                response.sendRedirect("/login.html");
            }
            return false;
        }

        private static boolean requiresAdmin(String path, String method) {
            if (path.startsWith("/adminpages/") || path.startsWith("/admin/")
                    || path.startsWith("/api/admin/") || path.startsWith("/delivery/update/")
                    || path.startsWith("/delivery/delete/")) {
                return true;
            }
            if (path.equals("/adminDashboard") || path.equals("/adminItems") || path.equals("/addItem")
                    || path.equals("/customOrders") || path.equals("/bakeryOrders") || path.equals("/adminInvoices")
                    || path.equals("/adminPayments") || path.equals("/adminRefunds")
                    || path.equals("/admin-refund-management")
                    || path.equals("/users") || path.equals("/userManagement")
                    || path.equals("/adminindex.html") || path.equals("/admin.html")
                    || path.equals("/adminitem") || path.equals("/adminitem.html")
                    || path.equals("/custome_order") || path.equals("/custome_orders")
                    || path.equals("/custome_orders.html") || path.equals("/admin-orders.html")
                    || path.equals("/admin-payments.html") || path.equals("/adminprofile.html")
                    || path.equals("/adminuserlist.html")) {
                return true;
            }
            if (path.equals("/api/cakeBookings") || path.startsWith("/api/cakeBookings/")) {
                return true;
            }
            if (path.equals("/api/orders") && "GET".equals(method)) {
                return true;
            }
            if (path.matches("/api/orders/\\d+/status") && "PUT".equals(method)) {
                return true;
            }
            if (path.equals("/api/payments") && "GET".equals(method)) {
                return true;
            }
            if ((path.matches("/api/payments/\\d+/status") && "PUT".equals(method))
                    || (path.matches("/api/payments/\\d+") && "DELETE".equals(method))) {
                return true;
            }
            if ((path.equals("/api/refund") && "GET".equals(method))
                    || (path.matches("/api/refund/\\d+") && ("PUT".equals(method) || "DELETE".equals(method)))) {
                return true;
            }
            return (path.equals("/api/cake-options") || path.matches("/api/cake-options/\\d+"))
                    && !"GET".equals(method);
        }
    }
}
