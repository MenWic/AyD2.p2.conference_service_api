# Base image for building the application
FROM gradle:jdk21-corretto

# Copy project files into the container
COPY . /app

# Copy the production configuration file
COPY src/main/resources/application.prod.properties /app/src/main/resources/application.properties

# Set the working directory
WORKDIR /app

# Build the application using Gradle
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# Serve the application
ENTRYPOINT ["java", "-jar", "build/libs/application.jar"]
