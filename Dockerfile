FROM openjdk:17.0

RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    gnupg \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Chrome 112.0.5615.49
RUN wget https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_112.0.5615.49-1_amd64.deb \
    && apt-get update \
    && apt install -y ./google-chrome-stable_112.0.5615.49-1_amd64.deb \
    && rm google-chrome-stable_112.0.5615.49-1_amd64.deb

# Install ChromeDriver 112.0.5615.49
RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/112.0.5615.49/chromedriver_linux64.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ \
    && rm /tmp/chromedriver.zip \
    && chmod +x /usr/local/bin/chromedriver

# Copy and build the application
COPY . .
RUN ./gradlew assemble
RUN cp build/libs/*.jar app.jar

# Expose port and run app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
