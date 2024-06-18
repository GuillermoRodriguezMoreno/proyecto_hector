FROM maven:3-amazoncorretto-21 AS build
COPY . .
RUN mvn clean package -DskipTests


FROM amazoncorretto:21-alpine
COPY --from=build /target/guestifyapi.jar guestifyapi.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","guestifyapi.jar"]