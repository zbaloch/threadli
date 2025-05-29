FROM openjdk:17-jre-slim
VOLUME /tmp
COPY target/threadli-web-0.0.1-SNAPSHOT.jar thradli.jar
ENTRYPOINT ["java","-jar","/thradli.jar"]