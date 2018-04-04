#!/bin/bash
gpio mode 22 out # diode en output
gpio mode 21 in # interrupteur en input
gpio mode 21 up # interrupteur pull-up
gpio write 22 0 # diode éteinte par défaut

nbBas=0
# on s'arrête si la pin est à l'état bas cinq fois de suite
while [ $nbBas -lt 5 ]; do
    if [ $(gpio read 21) -eq 0 ]; then
        let "nbBas += 1"
    else
        nbBas=0
    fi
    sleep 0.2
done
wall "Arrêt du système par GPIO !"
gpio write 22 1 # diode allumée pendant l'arrêt du système
pkill -f eurobotruck.jar && sleep 3 # on éteint le HL et on laisse un peu de temps au HL pour s'éteindre (s'il était déjà éteint, pas de sleep)
sudo halt
# la diode va être éteinte quand la raspi sera logiciellement arrêtée

#La pin 29 (GPIO 05), wiring pi 21, est reliée à l'interrupteur
#il y a un pull up, donc tu liras LOW quand on appuie sur le bouton
#et HIGH en temps normal

#La pin 31 (GPIO 06), wiring pi 22, permet d'alimenter une LED rouge
#il suffit de passer la pin en OUTPUT et de la passer HIGH pour allumer la DEL
#il y a déjà une DEL indiquant que la raspi est allumée, donc je pense qu'il faut utiliser cette DEL pour indiquer des infos concernant l'état de la rpi
#"en train de démarrer" "en train de s'éteindre" "peut être éteinte sans risque" "le code du HL tourne"
