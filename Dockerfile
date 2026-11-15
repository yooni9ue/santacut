FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl sqlite3 fonts-noto-cjk && rm -rf /var/lib/apt/lists/* && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar
RUN install -d -o app -g app /app/data
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -Djava.awt.headless=true"
ENTRYPOINT ["java","-jar","/app/app.jar"]

