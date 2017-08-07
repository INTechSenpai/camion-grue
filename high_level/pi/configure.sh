#!/bin/sh
sudo apt-get update -y
sudo apt-get upgrade -y
sudo apt-get dist-upgrade -y

sudo apt-get install -y git \
oracle-java8-jdk \
junit \
librxtx-java \
ant \
hostapd \
dnsmasq \
vim

# récupération du code
git clone https://github.com/INTechSenpai/moon-rover.git

# création du dépôt nu local
git init --bare moon-rover-rpi.git
cd moon-rover/pc
git remote set-url origin ~/moon-rover-rpi.git
git push

# compilation
ant

# personnalisation
sudo rm /etc/motd
sudo ln -s /home/pi/moon-rover/pc/intro.txt /etc/motd

# configuration hotspot wifi : https://frillip.com/using-your-raspberry-pi-3-as-a-wifi-access-point-with-hostapd/
sudo cp pi/dhcpcd.conf /etc/dhcpcd.conf
sudo cp pi/interfaces /etc/network/interfaces

sudo service dhcpcd restart
sudo ifdown wlan0
sudo ifup wlan0

sudo cp pi/hostapd.conf /etc/hostapd/hostapd.conf
sudo cp pi/hostapd /etc/default/hostapd
sudo cp pi/dnsmasq.conf /etc/dnsmasq.conf
sudo cp pi/sysctl.conf /etc/sysctl.conf
sudo cp pi/iptables.ipv4.nat /etc/iptables.ipv4.nat
sudo cp pi/rc.local /etc/rc.local

# ajout de l'adresse statique dans hosts
sudo sed -i s/127.0.1.1/172.24.1.1/g /etc/hosts

sudo reboot
