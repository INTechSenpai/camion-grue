#include "utils.h"

/*
 * La fonction '%' du langage C ne renvoie pas le modulo au sens mathématique du terme.
 * En réalité pour (a % b) il fait ( ABS(a) % b)*SIGNE(a)
 * Voici donc pour votre plus grand plaisir, la fonction modulo mathématiquement CORRECTE.
 *
 * De rien, c'est gratuit.
 *
 * @author: Sylvain Gaultier
 * */

int modulo(int nombre, int modulo)
{
	if(modulo <= 0)
	{
		return -1;
	}
	else if(nombre >= 0 )
	{
		return nombre % modulo;
	}
	else
	{
		return modulo - ((-nombre) % modulo);
	}
}

float fmodulo(float nombre, float modulo)
{
	float resultat = fmod(nombre, modulo);
	if (resultat < 0)
	{
		return resultat + modulo;
	}
	else
	{
		return resultat;
	}
}

// Met x au carré (plus optimisé que pow(x, 2) )
float square(float x)
{
	return x * x;
}