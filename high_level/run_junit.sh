#!/bin/sh
if [ "$#" -lt 1 ]; then
    echo "Usage: ./run.sh Classe#methode"
else
    sudo nice -n -10 ionice -c 1 java -Xmx1G -Xms1G -cp bin/:lib/hamcrest-core-1.3.jar:lib/junit.jar:lib/RXTXcomm.jar -Djava.library.path=/usr/lib/jni tests.JUnit_Test $@
fi
