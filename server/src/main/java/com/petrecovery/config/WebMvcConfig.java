package com.petrecovery.config;

import com.petrecovery.storage.UploadStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadStorageService uploadStorageService;
    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(UploadStorageService uploadStorageService, JwtInterceptor jwtInterceptor) {
        this.uploadStorageService = uploadStorageService;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadStorageService.getStoragePath() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/login/code",
                        "/api/post/search",
                        "/api/post/detail/**",
                        "/api/archive/search",
                        "/api/archive/detail/**",
                        "/api/stats/**"
                );
    }
}
