FROM eclipse-temurin:17-jre
COPY emotiondiary/emotiondiary-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8989
ENTRYPOINT ["java", "-jar", "/app.jar"]
