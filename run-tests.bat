@echo off
echo ========================================
echo Running E-Commerce Application Tests
echo ========================================

echo.
echo [1/4] Running Unit Tests...
call mvn test -Dtest="*Test" -DfailIfNoTests=false

echo.
echo [2/4] Running Integration Tests...
call mvn test -Dtest="*IntegrationTest" -DfailIfNoTests=false

echo.
echo [3/4] Running End-to-End Tests...
call mvn test -Dtest="*E2ETest" -DfailIfNoTests=false

echo.
echo [4/4] Running All Tests with Coverage...
call mvn test jacoco:report

echo.
echo ========================================
echo Test Execution Complete!
echo ========================================
echo.
echo Test Reports:
echo - Unit Test Results: target/surefire-reports/
echo - Coverage Report: target/site/jacoco/index.html
echo.
pause
