package com.example.skysport1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Boot mặc định CHỈ serve file tĩnh trong classpath:/static hoặc
 * classpath:/public — thư mục "uploads/" nằm ngoài classpath (ghi file lúc
 * runtime) nên phải khai báo resource handler thủ công, nếu không mọi ảnh
 * upload xong sẽ 404 khi trình duyệt gọi /uploads/...
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadRoot + "/");
    }
}