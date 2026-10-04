FROM node:22-alpine AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
COPY --from=frontend /workspace/src/main/resources/static/ ./src/main/resources/static/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=backend /workspace/target/agenda-odontologica-0.0.1-SNAPSHOT.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
USER app
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
