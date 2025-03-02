package itstep.learning.filters;

import com.google.inject.Singleton;
import itstep.learning.dal.dao.AccessTokenDao;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.dal.dto.UserAccess;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Singleton  // Применяем ко всем запроса
public class AuthFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());

    private final DataContext dataContext;
    private FilterConfig filterConfig;

    @Inject
    public AuthFilter(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        LOGGER.info("✅ AuthFilter инициализирован!");
    }

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) sreq;
        HttpServletResponse resp = (HttpServletResponse) sresp;

        String authStatus;  // переменная для хранения статуса авторизации

        // 1) Проверяем наличие заголовка Authorization
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            authStatus = "Authorization header required";
            LOGGER.warning(authStatus);

            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"message\":\"" + authStatus + "\"}");
            ((HttpServletResponse) resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            // Сохраняем статус в атрибут запроса (если нужно обработать дальше)
            req.setAttribute("authStatus", authStatus);
            return;
        }

        // 2) Проверяем схему Bearer
        String authScheme = "Bearer ";
        if (!authHeader.startsWith(authScheme)) {
            authStatus = "Authorization scheme error";
            LOGGER.warning(authStatus);

            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"message\":\"" + authStatus + "\"}");
            ((HttpServletResponse) resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            req.setAttribute("authStatus", authStatus);
            return;
        }

        // 3) Извлекаем токен
        String credentials = authHeader.substring(authScheme.length());
/*
        // 4) Получаем UserAccess
        UserAccess userAccess = dataContext
                .getAccessTokenDao()
                .getUserAccess(credentials);

        if (userAccess == null) {
            authStatus = "Token expires or invalid";
            LOGGER.warning(authStatus);

            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"message\":\"" + authStatus + "\"}");
            ((HttpServletResponse) resp).setStatus(HttpServletResponse.SC_FORBIDDEN);

            req.setAttribute("authStatus", authStatus);
            return;
        }

        // Если всё ок, сохраняем статус "OK" и userAccess
        authStatus = "OK";
        req.setAttribute("authStatus", authStatus);
        req.setAttribute("authUserAccess", userAccess);

        // Пропускаем запрос дальше
        chain.doFilter(sreq, sresp);
    }

    @Override
    public void destroy() {
        LOGGER.info("❌ AuthFilter уничтожен!");
    }
}

 */
    }
}