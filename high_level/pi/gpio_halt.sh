#!/bin/bash
if [ "$#" -ne 2 ]; then
    echo "Usage: ./gpio_halt.sh pinDiode pinHalt"
else
    gpio mode $1 out # diode en output
    gpio mode $2 down # interrupteur pull-down
    gpio write $1 1 # allumage de la diode témoin

    # on s'arrête si la pin passe à l'état haut
    # en cas de problème, vu qu'on est en pull-down on ne s'éteint pas
    while [ $(gpio read $2) -eq 0 ]
    do
        sleep 2
    done
    # la diode s'éteint car la raspberry est coupée
    echo "Arrêt du système par GPIO !"
    halt
fi
