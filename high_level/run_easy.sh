#!/bin/sh
if [ "$#" -lt 1 ]; then
    echo "Usage: ./run.sh Classe"
else
    java -cp bin/:config/core/lib/ini4j-0.5.5-SNAPSHOT-jdk14.jar:graphic-toolbox/core/lib/jcommon-1.0.13.jar:graphic-toolbox/core/lib/jfreechart-1.0.13.jar $@
fi
