FROM amazoncorretto:17-alpine-jdk

ADD target/*.jar app.jar

RUN apk --no-cache add curl
RUN apk add --no-cache bind-tools

RUN echo "nameserver 8.8.8.8" > /etc/resolv.conf
RUN echo "nameserver 8.8.4.4" >> /etc/resolv.conf


ENTRYPOINT ["java", "-Djsse.enableSNIExtension=false" , "-Djava.net.preferIPv4Stack=true", "sun.net.spi.nameservice.nameservers=8.8.8.8,1.1.1.1", "sun.net.spi.nameservice.provider.1=dns,sun", "-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
