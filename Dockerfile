FROM openjdk:17-jre-slim
VOLUME /tmp
COPY target/threadli-*.jar thradli.jar
ENTRYPOINT ["java","-jar","/thradli.jar"]