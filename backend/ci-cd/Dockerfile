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
# 중첩 jar를 풀어 둬야 JVM AOT 캐시가 클래스 경로를 그대로 쓸 수 있다(lib/ + app.jar). 풀린 jar 이름은 원본 이름을 따른다.
RUN cp build/libs/*-SNAPSHOT.jar app.jar && java -Djarmode=tools -jar app.jar extract --destination /workspace/extracted

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl sqlite3 fonts-noto-cjk && rm -rf /var/lib/apt/lists/* && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
# 의존성(lib/)은 앱 코드보다 덜 바뀌므로 먼저 복사해 레이어를 나눈다.
COPY --from=build /workspace/extracted/lib /app/lib
COPY --from=build /workspace/extracted/app.jar /app/app.jar
# JVM AOT 캐시(JEP 483·514). 컨텍스트를 한 번 띄웠다 닫으며(spring.context.exit=onRefresh) 읽은 클래스를 기록하고 캐시로 만든다.
# 기동 26초 → 12초, 메타스페이스·클래스 공간·심볼 약 120MB가 공유 캐시로 옮겨 유휴 메모리가 준다(deployment.md 메모리 예산).
# 학습은 실제 런타임 이미지에서 해야 JVM이 같다. DB·키는 학습용 자리표시 값이고, 만든 뒤 /tmp를 지운다.
# 캐시가 맞지 않으면(JVM·클래스 경로 변경) JVM은 경고만 남기고 캐시 없이 뜬다.
RUN JAVA_TOOL_OPTIONS= GEMINI_API_KEY=aot-training-placeholder SPRING_DATASOURCE_URL=jdbc:sqlite:/tmp/aot-training.db       java -XX:+UseSerialGC -XX:AOTMode=record -XX:AOTConfiguration=/tmp/app.aotconf -Dspring.context.exit=onRefresh -jar /app/app.jar     && JAVA_TOOL_OPTIONS= java -XX:+UseSerialGC -XX:AOTMode=create -XX:AOTConfiguration=/tmp/app.aotconf -XX:AOTCache=/app/app.aot -jar /app/app.jar     && test -s /app/app.aot && rm -rf /tmp/* /tmp/.[!.]*
RUN install -d -o app -g app /app/data
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -Djava.awt.headless=true"
ENTRYPOINT ["java","-XX:AOTCache=/app/app.aot","-jar","/app/app.jar"]
