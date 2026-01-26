# 1. 使用 Eclipse Temurin (最穩定的 OpenJDK 發行版之一)
FROM eclipse-temurin:17-jdk-jammy

# 2. 設定工作目錄
WORKDIR /app

# 3. 將 Maven 打包好的 JAR 檔複製進容器
# 注意：這裡假設您的 jar 檔生成在 target 目錄下，且名稱包含 flash-sale
COPY target/*.jar app.jar

# 4. 設定啟動指令
ENTRYPOINT ["java", "-jar", "app.jar"]

# 5. 暴露 8080 port
EXPOSE 8080