package itstep.learning.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Фільтр для встановлення кодування запитів і відповідей у формат UTF-8.
 */
@WebFilter("/*")
public class CharsetFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CharsetFilter.class.getName());
    private String charset;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Чтение параметра charset из конфигурации фильтра, если задан, иначе использовать UTF-8
        charset = filterConfig.getInitParameter("charset");
        if (charset == null || charset.trim().isEmpty()) {
            charset = "UTF-8";
        }
        LOGGER.info("CharsetFilter инициализирован с кодировкой: " + charset);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(charset);
        response.setCharacterEncoding(charset);
        // Установка contentType, если он не установлен ранее
        if (response.getContentType() == null) {
            response.setContentType("text/html; charset=" + charset);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOGGER.info("CharsetFilter уничтожен!");
    }
}