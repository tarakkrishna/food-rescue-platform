FROM openjdk:17-jdk-slim

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests

EXPOSE 10000

CMD ["java", "-jar", "target/food-rescue-backend-1.0.0.jar"]
