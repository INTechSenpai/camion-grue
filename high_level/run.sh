#!/bin/sh
if [ "$#" -lt 1 ]; then
    echo "Usage: ./run.sh Classe"
else
    sudo killall -9 java
    sudo rm -f /var/lock/LCK..tty*
    sudo nice -n -10 ionice -c 1 java -Xmx1G -Xms1G -cp bin/:lib/RXTXcomm.jar -Djava.library.path=/usr/lib/jni $@
fi
