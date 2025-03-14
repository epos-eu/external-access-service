# Use Amazon Corretto JDK 17 with Alpine as the base image
FROM amazoncorretto:17-alpine-jdk

# Set environment variables for DNS resolution (optional but useful)
ENV DNSMASQ_CONF=/etc/dnsmasq.conf

# Add application JAR
ADD target/*.jar app.jar

# Install required packages in a single RUN command (best practice)
RUN apk add --no-cache \
    curl \
    bind-tools \
    dnsmasq \
    ca-certificates


# Ensure dnsmasq starts before the Java application
CMD dnsmasq -k & java -Djava.security.egd=file:/dev/./urandom -jar /app.jar
