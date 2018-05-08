# Après le moon rover, INTech Senpaï est fier de vous présenter son camion grue !

![Affiche robot](https://raw.githubusercontent.com/INTechSenpai/eurobotruck/master/resources/camion-grue-affiche.jpg)

## INTech Senpaï ?

INTech Senpaï participe pour la troisième fois à la Coupe de France de Robotique. Après le succès du Robot Sumo et du moon rover, qui a fini 29e à la coupe 2017, nous revenons en force avec le camion grue.

## Un modèle réduit unique

Notre camion grue est un modèle réduit à l'échelle 1:16 des immenses engins qui ont bercé notre enfance. À taille réelle, il mesurerait 3×7m ! Son design avec ses six roues (dont quatre directrices et motrices), ses feux de recul, de stop, de nuit, ses clignotants, ses gyrophare, sa grue est unique : il a été entièrement conçu par notre équipe.

## Un camion autonome

Notre camion grue utilise le système Kraken de déplacement autonome, fruit de plus de trois ans de R&D d'un doctorant en intelligence artificielle. Contrairement aux voitures autonomes qui suivent simplement une route, les engins de chantier évoluent dans un environnement encombré et changeant ; trouver un chemin implique un grand nombre de manœuvres qui peuvent être difficiles à trouver, même pour un humain. C'est ce que réalisé le système Kraken, qui permet au camion grue de se déplacer avec aisance sur la table de jeux qui représente assez bien ce type d'environnement.

Le système Kraken est un logiciel libre et en [libre accès](https://github.com/PFGimenez/The-Kraken-Pathfinding).

## Un concurrent à la Coupe de France

Enfin, le camion grue est également un compétiteur de la Coupe. Sa grue terminée par une pince lui permet de manipuler des cubes et de les empiler.

![Prise d'un cube](https://raw.githubusercontent.com/INTechSenpai/eurobotruck/master/resources/cube.jpg)

## Hardware

Le camion grue est équipé de quatre unités de calcul : trois Teensy pour le contrôle bas niveau et une Raspberry Pi 3+ pour Kraken. Avec ses treize capteurs _time of flight_ dont deux rotatifs, il voit tout son environnement.

![Capteurs ToF](https://raw.githubusercontent.com/INTechSenpai/eurobotruck/master/resources/ToF.jpg)

![Mécanisme d'ouverture](https://raw.githubusercontent.com/INTechSenpai/eurobotruck/master/resources/ouverture.jpg)

![Électronique](https://raw.githubusercontent.com/INTechSenpai/eurobotruck/master/resources/elec.jpg)

## L'équipe

[Sylvain Gaultier](https://github.com/sylvaing19) est le fondateur d'INTech Senpaï, il s'occupe de la conception et de la réalisation de la mécanique et de l'électronique du camion grue. Il a également programmé les trois Teensy.

 [Pierre-François Gimenez](https://github.com/PFGimenez), doctorant en Intelligence Artificielle, a été recruté par Sylvain pour son projet de développement d'un camion grue autonome. Il est le concepteur et le programmeur du système Kraken.
