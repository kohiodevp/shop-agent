FROM eclipse-temurin:17-jdk

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl unzip git python3 python3-venv build-essential \
    && rm -rf /var/lib/apt/lists/*

# Variables Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Installer Android command-line tools (compatible JDK 17)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && cd /tmp \
    && curl -fsSL -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
    && unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm -f /tmp/cmdline-tools.zip

# Accepter les licences et installer les paquets SDK requis
RUN yes | sdkmanager --licenses >/dev/null 2>&1 \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null 2>&1 \
    && echo "SDK installed"

# --- Gradle 8.0.2 (compatible AGP 8.0 / Compose 1.4) ---
RUN cd /opt \
    && curl -fsSL -o gradle.zip https://services.gradle.org/distributions/gradle-8.0.2-bin.zip \
    && unzip -q gradle.zip \
    && rm -f gradle.zip \
    && ln -s /opt/gradle-8.0.2/bin/gradle /usr/local/bin/gradle
ENV GRADLE_USER_HOME=/opt/gradle-home

WORKDIR /workspace

COPY . /workspace

# Generer un keystore de release reproductible (pour APK signe)
RUN keytool -genkeypair -v \
    -keystore /workspace/release.keystore \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias shop -storepass shop123 -keypass shop123 \
    -dname "CN=Shop Agent, OU=BOS, O=Betsaleel, L=Bobo-Dioulasso, ST=Hauts-Bassins, C=BF" \
    && echo "KEYSTORE generated"

# Build de l'APK au moment du build de l'image
RUN gradle app:assembleRelease --no-daemon --stacktrace \
    || ./gradlew app:assembleRelease --no-daemon --stacktrace
