#!/bin/sh
mvn clean compile assembly:single
scp target/eurobotruck.jar pi@camion-grue:
rsync -a paths pi@camion-grue:
