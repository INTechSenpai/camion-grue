#!/bin/sh
mvn clean compile assembly:single
scp target/eurobotruck.jar pi@172.24.1.1:
scp -r paths/ pi@172.24.1.1:
