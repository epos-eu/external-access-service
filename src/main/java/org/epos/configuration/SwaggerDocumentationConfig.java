package org.epos.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@Configuration
public class SwaggerDocumentationConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
            .info(new Info()
                .title("External Access Service RESTful APIs")
                .description("This is the External Access Service RESTful APIs Swagger page.")
                .termsOfService("")
                .version(System.getenv("VERSION"))
                .license(new License()
                    .name("MIT License")
                    .url("https://epos-ci.brgm.fr/epos/WebApi/raw/master/LICENSE"))
                .contact(new io.swagger.v3.oas.models.info.Contact()
                    .email("apis@lists.epos-ip.org")));
    }

}
