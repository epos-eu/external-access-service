FROM amazoncorretto:17-alpine-jdk

ADD target/*.jar app.jar

RUN apk --no-cache add curl
RUN apk add --no-cache bind-tools


ENTRYPOINT ["java", "-Djsse.enableSNIExtension=false", "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
