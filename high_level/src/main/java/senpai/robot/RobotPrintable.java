package senpai.robot;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import javax.imageio.ImageIO;
import pfg.config.Config;
import pfg.graphic.GraphicPanel;
import pfg.graphic.printable.Printable;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XY_RW;
import senpai.utils.ConfigInfoSenpai;

public class RobotPrintable implements Printable
{
	private transient Image imageRobot;
	private Cinematique cinematique;
	protected transient double cos, sin;
	
	private static final long serialVersionUID = 1L;

	// Longueur entre le centre et un des coins
	protected transient double demieDiagonale;
	protected transient XY_RW centreGeometrique;
	protected transient double demieDiagonaleImg;
	
	// calcul des positions des coins
	// ces coins sont dans le repère de l'obstacle !
	protected transient XY_RW coinBasGauche;
	protected transient XY_RW coinHautGauche;
	protected transient XY_RW coinBasDroite;
	protected transient XY_RW coinHautDroite;

	// ces coins sont dans le repère de la table
	protected transient XY_RW coinBasGaucheRotate;
	protected transient XY_RW coinHautGaucheRotate;
	protected transient XY_RW coinBasDroiteRotate;
	protected transient XY_RW coinHautDroiteRotate;
	

	protected transient XY_RW coinBasGaucheImg;
	protected transient XY_RW coinHautGaucheImg;
	protected transient XY_RW coinBasDroiteImg;
	protected transient XY_RW coinHautDroiteImg;

	protected transient XY_RW coinBasGaucheRotateImg;
	protected transient XY_RW coinHautGaucheRotateImg;
	protected transient XY_RW coinBasDroiteRotateImg;
	protected transient XY_RW coinHautDroiteRotateImg;

	public RobotPrintable(Config config)
	{
		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);
		int marge = config.getInt(ConfigInfoSenpai.MARGE_PATHFINDING);
		
		centreGeometrique = new XY_RW();
		coinBasGaucheRotate = new XY_RW();
		coinHautGaucheRotate = new XY_RW();
		coinBasDroiteRotate = new XY_RW();
		coinHautDroiteRotate = new XY_RW();
		coinBasGaucheRotateImg = new XY_RW();
		coinHautGaucheRotateImg = new XY_RW();
		coinBasDroiteRotateImg = new XY_RW();
		coinHautDroiteRotateImg = new XY_RW();

		int a = demieLargeurNonDeploye + marge;
		int b = demieLargeurNonDeploye + marge;
		int c = demieLongueurAvant;
		int d = demieLongueurArriere;

		int a2 = demieLargeurNonDeploye;
		int b2 = demieLargeurNonDeploye;
		int c2 = demieLongueurAvant;
		int d2 = demieLongueurArriere;

		coinBasGauche = new XY_RW(-d, -a);
		coinHautGauche = new XY_RW(-d, b);
		coinBasDroite = new XY_RW(c, -a);
		coinHautDroite = new XY_RW(c, b);

		coinBasGaucheImg = new XY_RW(-d2, -a2);
		coinHautGaucheImg = new XY_RW(-d2, b2);
		coinBasDroiteImg = new XY_RW(c2, -a2);
		coinHautDroiteImg = new XY_RW(c2, b2);

		demieDiagonale = Math.sqrt((a + b) * (a + b) / 4 + (c + d) * (c + d) / 4);
		demieDiagonaleImg = Math.sqrt((a2 + b2) * (a2 + b2) / 4 + (c2 + d2) * (c2 + d2) / 4);
		centreGeometrique = new XY_RW();
		
		try
		{
			imageRobot = ImageIO.read(getClass().getResourceAsStream(config.getString(ConfigInfoSenpai.GRAPHIC_ROBOT_PATH)));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void print(Graphics g, GraphicPanel f)
	{
		if(cinematique == null)
			return;
		
		cos = Math.cos(cinematique.orientationReelle);
		sin = Math.sin(cinematique.orientationReelle);
		convertitVersRepereTable(coinBasGauche, coinBasGaucheRotate);
		convertitVersRepereTable(coinHautGauche, coinHautGaucheRotate);
		convertitVersRepereTable(coinBasDroite, coinBasDroiteRotate);
		convertitVersRepereTable(coinHautDroite, coinHautDroiteRotate);
		convertitVersRepereTable(coinBasGaucheImg, coinBasGaucheRotateImg);
		convertitVersRepereTable(coinHautGaucheImg, coinHautGaucheRotateImg);
		convertitVersRepereTable(coinBasDroiteImg, coinBasDroiteRotateImg);
		convertitVersRepereTable(coinHautDroiteImg, coinHautDroiteRotateImg);
		coinBasDroiteRotate.copy(centreGeometrique);
		centreGeometrique = centreGeometrique.plus(coinHautGaucheRotate).scalar(0.5);
		
		if(coinBasDroiteRotate == null)
			return;

/*		int[] X = new int[4];
		X[0] = (int) coinBasDroiteRotate.getX();
		X[1] = (int) coinHautDroiteRotate.getX();
		X[2] = (int) coinHautGaucheRotate.getX();
		X[3] = (int) coinBasGaucheRotate.getX();

		int[] Y = new int[4];
		Y[0] = (int) coinBasDroiteRotate.getY();
		Y[1] = (int) coinHautDroiteRotate.getY();
		Y[2] = (int) coinHautGaucheRotate.getY();
		Y[3] = (int) coinBasGaucheRotate.getY();

		for(int i = 0; i < 4; i++)
		{
			X[i] = f.XtoWindow(X[i]);
			Y[i] = f.YtoWindow(Y[i]);
		}
		g.fillPolygon(X, Y, 4);*/

		if(imageRobot != null)
		{
			Graphics2D g2d = (Graphics2D) g;
			AffineTransform trans = new AffineTransform();
			trans = new AffineTransform();
			trans.setTransform(new AffineTransform());
			trans.translate(f.XtoWindow(cinematique.getPosition().getX()), f.YtoWindow(cinematique.getPosition().getY()));
			trans.rotate(-cinematique.orientationReelle);
			trans.translate(f.distanceXtoWindow((int) coinHautGaucheImg.getX()), f.distanceYtoWindow(-(int) coinHautGaucheImg.getY()));
			trans.scale(1. * f.distanceXtoWindow((int) coinHautGaucheImg.distance(coinHautDroiteImg)) / imageRobot.getWidth(null), 1. * f.distanceXtoWindow((int) coinHautGaucheImg.distance(coinBasGaucheImg)) / imageRobot.getHeight(null));
			g2d.drawImage(imageRobot, trans, null);
		}		
	}
	
	
/*	private void convertitVersRepereRobot(XY point, XY_RW out)
	{
		out.setX(cos * (point.getX() - cinematique.getPosition().getX()) + sin * (point.getY() - cinematique.getPosition().getY()));
		out.setY(-sin * (point.getX() - cinematique.getPosition().getX()) + cos * (point.getY() - cinematique.getPosition().getY()));
	}*/

	/**
	 * Rotation dans le sens +angle
	 * Passe du repère de l'obstacle au repère de la table
	 * 
	 * @param point
	 * @return
	 */
	private void convertitVersRepereTable(XY point, XY_RW out)
	{
		out.setX(cos * point.getX() - sin * point.getY() + cinematique.getPosition().getX());
		out.setY(sin * point.getX() + cos * point.getY() + cinematique.getPosition().getY());
	}

	public void initPositionObject(Cinematique c)
	{
		cinematique = c;
	}

	public Cinematique getCinematique()
	{
		return cinematique.clone();
	}
}
