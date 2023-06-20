package org.epos.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@Configuration
public class SwaggerDocumentationConfig {

    @Bean
    public Docket customImplementation(){
        return new Docket(DocumentationType.OAS_30)
                .select()
                    .apis(RequestHandlerSelectors.basePackage("org.epos.api"))
                    .build()
                .directModelSubstitute(org.threeten.bp.LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(org.threeten.bp.OffsetDateTime.class, java.util.Date.class)
                .apiInfo(apiInfo());
    }

    ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("External Access Service RESTful APIs")
            .description("This is the External Access Service RESTful APIs Swagger page.")
            .license("MIT License")
            .licenseUrl("https://epos-ci.brgm.fr/epos/WebApi/raw/master/LICENSE")
            .termsOfServiceUrl("")
            .version(System.getenv("VERSION"))
            .contact(new Contact("","", "apis@lists.epos-ip.org"))
            .build();
    }

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
