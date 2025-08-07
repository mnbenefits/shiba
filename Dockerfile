FROM registry.redhat.io/openjdk/openjdk-21-rhel8
COPY . .
RUN ./gradlew assemble
RUN cp build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","/app.jar"]