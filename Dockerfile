FROM amazoncorretto:17-alpine-jdk

ADD target/*.jar app.jar

RUN apk --no-cache add curl

ENTRYPOINT ["java", "-Djsse.enableSNIExtension=false" , "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
