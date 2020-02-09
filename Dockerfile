FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/aion-faucet-web-*.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]