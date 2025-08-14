# 빌드시
FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean bootJar --no-daemon

# 런타임시
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /workspace/build/libs/liberarium-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
