@echo off
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD=admin1234
cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement
mvnw.cmd spring-boot:run -DskipTests
