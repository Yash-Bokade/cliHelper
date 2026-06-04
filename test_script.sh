#!/bin/bash
(
sleep 1
echo "help"
sleep 1
echo "export MY_VAR=hello_world"
sleep 1
echo "history"
sleep 1
echo "reload"
sleep 1
echo "exit"
) | java -jar build/libs/cliHelper-1.0-SNAPSHOT.jar
