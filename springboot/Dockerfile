# Per convention 18 (base image bumped to 21 — the project's Java toolchain).
FROM eclipse-temurin:21-jdk

ARG JAR_FILE_PATH=build/libs/pickupdrop-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE_PATH} api.jar

ENTRYPOINT ["java", "-jar", "-Djava.net.preferIPv4Stack=true", "api.jar"]
