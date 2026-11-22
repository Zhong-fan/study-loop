package com.studyloop.common.internal;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;

/**
 * 内部调用凭证的自动装配。
 * <p>
 * 通过 `META-INF/spring/...AutoConfiguration.imports` 生效，
 * 各服务不用改代码，只要在 yml 里打开 `app.internal.enabled`。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.internal.enabled", havingValue = "true")
public class InternalAuthAutoConfiguration {

    private static final String DEFAULT_TOKEN = "dev-internal-token";

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(InternalAuthAutoConfiguration.class);

    /** 服务端：校验进来的请求 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletConfig {

        @Bean
        public FilterRegistrationBean<InternalTokenFilter> internalTokenFilter(
                @Value("${app.internal.token:}") String token) {
            FilterRegistrationBean<InternalTokenFilter> bean = new FilterRegistrationBean<>();
            if (DEFAULT_TOKEN.equals(token)) {
                LOG.warn("INTERNAL_TOKEN 未设置，正在使用默认值；部署前必须覆盖");
            }
            bean.setFilter(new InternalTokenFilter(token));
            bean.addUrlPatterns("/*");
            bean.setOrder(-100);
            return bean;
        }
    }

    /** 客户端：Feign 调兄弟服务时自动带上 token */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RequestInterceptor.class)
    static class FeignConfig {

        @Bean
        public RequestInterceptor internalTokenInterceptor(
                @Value("${app.internal.token:}") String token) {
            return template -> template.header(InternalTokenFilter.HEADER, token);
        }
    }
}
