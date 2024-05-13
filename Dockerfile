FROM amazoncorretto:17-alpine-jdk

ADD target/*.jar app.jar

RUN apk --no-cache add curl

ENTRYPOINT ["java","-Dlog4j.configurationFile=/etc/log4j2/log4j2.properties", "-Djsse.enableSNIExtension=false" , "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
