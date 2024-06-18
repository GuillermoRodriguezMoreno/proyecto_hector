FROM maven:3-amazoncorretto-21 AS build
COPY . .
RUN mvn clean package -DskipTests


FROM amazoncorretto:21-alpine
COPY --from=build /target/api_rest.jar api_rest.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","api_rest.jar"]