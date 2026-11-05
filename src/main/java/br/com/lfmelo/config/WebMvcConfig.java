package br.com.lfmelo.config;

import br.com.lfmelo.security.TenantAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the {@link TenantAccessInterceptor} for all condominium-scoped endpoints. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAccessInterceptor tenantAccessInterceptor;

    public WebMvcConfig(TenantAccessInterceptor tenantAccessInterceptor) {
        this.tenantAccessInterceptor = tenantAccessInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry
                .addInterceptor(tenantAccessInterceptor)
                .addPathPatterns("/api/v1/condominiums", "/api/v1/condominiums/**");
    }
}
