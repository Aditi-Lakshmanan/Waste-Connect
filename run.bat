@echo off
set JAVA_HOME=C:\Users\HP\.jdks\openjdk-26.0.2
set PATH=%JAVA_HOME%\bin;C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin;%PATH%
mvn clean javafx:run
