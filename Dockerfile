FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the JAR file into the container
COPY ./target/PolicyManagement-0.0.1-SNAPSHOT.jar /app/PolicyManagement-0.0.1-SNAPSHOT.jar

# Expose the port that the application runs on
EXPOSE 8084

# Command to run the application
CMD ["java", "-jar", "PolicyManagement-0.0.1-SNAPSHOT.jar"]
