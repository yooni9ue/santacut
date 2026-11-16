# syntax=docker/dockerfile:1
# 빌드 컨텍스트는 backend/ 디렉터리다.
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace
# 의존성을 레이어에 남겨야 GitHub Actions 레이어 캐시(type=gha)에 함께 저장된다.
# 캐시 마운트는 러너가 바뀌면 비어 있으므로 CI에서는 효과가 없다.
ENV GRADLE_USER_HOME=/workspace/.gradle-home     GRADLE_OPTS="-Dorg.gradle.internal.http.connectionTimeout=60000 -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.repository.max.retries=5 -Dorg.gradle.internal.repository.initial.backoff=2000"
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew
# 빌드 파일이 바뀔 때만 다시 받는다. 일시적인 네트워크 오류는 두 번 더 시도한다.
RUN for i in 1 2 3; do ./gradlew --no-daemon --quiet dependencies > /dev/null && exit 0; echo "의존성 받기 재시도 $i" >&2; sleep $((i * 15)); done; exit 1
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
