@echo off
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
set /p CP=<cp.txt
java -cp "target\classes;%CP%" demo.SimpleHttpServer
