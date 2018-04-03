#!/bin/bash
gpio mode 22 out # diode en output
gpio mode 21 in # interrupteur en input
gpio mode 21 up # interrupteur pull-up
gpio write 22 0 # diode éteinte par défaut

# on s'arrête si la pin passe à l'état bas
while [ $(gpio read 21) -eq 1 ]
do
    sleep 0.2
done
wall "Arrêt du système par GPIO !"
halt

#La pin 29 (GPIO 05), wiring pi 21, est reliée à l'interrupteur
#il y a un pull up, donc tu liras LOW quand on appuie sur le bouton
#et HIGH en temps normal

#La pin 31 (GPIO 06), wiring pi 22, permet d'alimenter une LED rouge
#il suffit de passer la pin en OUTPUT et de la passer HIGH pour allumer la DEL
#il y a déjà une DEL indiquant que la raspi est allumée, donc je pense qu'il faut utiliser cette DEL pour indiquer des infos concernant l'état de la rpi
#"en train de démarrer" "en train de s'éteindre" "peut être éteinte sans risque" "le code du HL tourne"
