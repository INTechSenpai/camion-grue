#!/bin/sh
if [ "$#" -lt 1 ]; then
    echo "Usage: ./run.sh Classe"
else
    java -cp bin/:lib/RXTXcomm.jar -Djava.library.path=/usr/lib/jni $@
fi
