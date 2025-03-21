package itstep.learning.filters;

import jakarta.inject.Singleton;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Фільтр для встановлення кодування запитів і відповідей у формат UTF-8.
 */
@Singleton

public class CharsetFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CharsetFilter.class.getName());
    private String charset = "UTF-8";  // Default

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            LOGGER.info("➡️ CharsetFilter init() вызван");

            if (filterConfig != null) {
                String paramCharset = filterConfig.getInitParameter("charset");
                if (paramCharset != null && !paramCharset.trim().isEmpty()) {
                    charset = paramCharset;
                }
            }

            LOGGER.info("✅ CharsetFilter инициализирован с кодировкой: " + charset);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Ошибка инициализации CharsetFilter", e);
            throw new ServletException("Ошибка CharsetFilter", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            LOGGER.info("➡️ CharsetFilter doFilter() вызван");

            request.setCharacterEncoding(charset);
            response.setCharacterEncoding(charset);

            String contentType = response.getContentType();
            LOGGER.info("ℹ️ Content-Type перед проверкой: " + contentType);

            if (contentType == null || contentType.isBlank()) {
                response.setContentType("text/html; charset=" + charset);
                LOGGER.info("ℹ️ Content-Type был пустой, установлен: text/html; charset=" + charset);
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Ошибка в CharsetFilter doFilter", e);
            throw new ServletException("Ошибка в CharsetFilter", e);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("🛑 CharsetFilter уничтожен!");
    }
}