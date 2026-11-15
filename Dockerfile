FROM node:24-alpine AS frontend-build
WORKDIR /workspace
COPY package.json package-lock.json ./
COPY frontend/package.json ./frontend/package.json
RUN npm ci
COPY frontend ./frontend
RUN npm run build

FROM eclipse-temurin:25-jdk-jammy AS backend-build
WORKDIR /workspace/backend
COPY backend/gradlew backend/gradlew.bat backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/gradle ./gradle
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl sqlite3 fonts-noto-cjk && rm -rf /var/lib/apt/lists/* && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=backend-build /workspace/backend/build/libs/*-SNAPSHOT.jar /app/app.jar
RUN install -d -o app -g app /app/data
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -Djava.awt.headless=true"
ENTRYPOINT ["java","-jar","/app/app.jar"]
