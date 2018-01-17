#!/bin/sh
sudo apt-get update -y
sudo apt-get upgrade -y
sudo apt-get dist-upgrade -y

sudo apt-get install -y git \
zsh \
oracle-java8-jdk \
junit \
librxtx-java \
ant \
hostapd \
dnsmasq \
nvim

# zsh par défaut
chsh -s /usr/bin/zsh

# oh-my-zsh
sh -c "$(curl -fsSL https://raw.githubusercontent.com/robbyrussell/oh-my-zsh/master/tools/install.sh)"

git clone https://github.com/PFGimenez/dotfiles.git /tmp/dotfiles
cp /tmp/dotfiles/.zshrc .
# désactivation de la mise à jour de oh-my-zsh
sed -i '1s/^/DISABLE_AUTO_UPDATE="true"\n/' .zshrc
cp /tmp/dotfiles/defaults.vim .
mkdir .config/nvim
cp /tmp/dotfiles/.config/nvim/init.vim .config/nvim/

# récupération du code
git clone https://github.com/PFGimenez/config.git
git clone https://github.com/PFGimenez/The-Kraken-Pathfinding.git
git clone https://github.com/PFGimenez/dependency-injector.git
git clone https://github.com/PFGimenez/graphic-toolbox.git
git clone https://github.com/INTechSenpai/eurobotruck.git

# création du dépôt nu local
git init --bare config-rpi.git
cd config
git remote set-url origin ~/config-rpi.git
git push

git init --bare kraken-rpi.git
cd The-Kraken-Pathfinding
git remote set-url origin ~/kraken-rpi.git
git push

git init --bare injector-rpi.git
cd dependency-injector
git remote set-url origin ~/injector-rpi.git
git push

git init --bare graphic-rpi.git
cd graphic-toolbox
git remote set-url origin ~/graphic-rpi.git
git push

git init --bare eurobotruck-rpi.git
cd eurobotruck
git remote set-url origin ~/eurobotruck-rpi.git
git push

# compilation
cd high_level
./all_update.sh

# personnalisation
sudo rm /etc/motd
sudo ln -s /home/pi/eurobotruck/high_level/src/main/resources/intro.txt /etc/motd

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
