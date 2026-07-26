# Per convention 18 (base image bumped to 21 — the project's Java toolchain).
FROM eclipse-temurin:21-jdk

# Timezone must be set before the JVM starts: the JDBC driver snapshots the
# default zone early, and a later TimeZone.setDefault() leaves the driver on
# UTC while Hibernate uses Asia/Seoul — which shifts DATE values by a day.
ENV TZ=Asia/Seoul

ARG JAR_FILE_PATH=build/libs/pickupdrop-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE_PATH} api.jar

ENTRYPOINT ["java", "-jar", "-Djava.net.preferIPv4Stack=true", "-Duser.timezone=Asia/Seoul", "api.jar"]
