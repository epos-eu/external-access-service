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
    dnsmasq

# Copy a custom dnsmasq.conf file (optional: create it locally)
COPY dnsmasq.conf /etc/dnsmasq.conf

# Ensure dnsmasq starts before the Java application
CMD dnsmasq -k & java -Djsse.enableSNIExtension=false -Djava.security.egd=file:/dev/./urandom -jar /app.jar
