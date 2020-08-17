package hexRail;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JPanel;


public class MapDisplay extends JPanel {
	private static final long serialVersionUID = 1L;
	private WorldOfChooChoo infoService;
	//Values for knowing what "world" we have to work with
	private int			screenWidth		= 1000;
	private int			screenHeight	= 700;
	private BufferedImage theImage		= new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
	
	//Settings of world displaying
	private boolean	discreteWithinTerrainTypes, showGrid;
	private Color[]	terrainMin	= {new Color(0,0,100), new Color(0,0,100),		new Color(255, 230, 130),	new Color(0,50,0),		Color.DARK_GRAY, Color.WHITE};
	private Color[]	terrainMax	= {new Color(0,0,100), new Color(150,150,250),	new Color(255, 230, 130),	new Color(0,255,0),		Color.GRAY,		Color.WHITE};
	
	public MapDisplay(WorldOfChooChoo info) {
		infoService = info;
	}
	
	public void paintComponent(Graphics g) {
		g.drawImage(theImage, 0, 0, screenWidth, screenHeight, null);
	}
	
	public void draw() {
		Color pixelColor;
		Graphics2D g2d = theImage.createGraphics();
		//Clear everything - perhaps optimization could be to keep layers separate?
		g2d.setStroke(new BasicStroke(1));
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, screenWidth, screenHeight);
		
		int hexCols = infoService.getHexCols();
		int hexRows = infoService.getHexRows();
		
		//DRAW ALL THE UNDERLYING TERRAIN HEXES
		double unitHalfWidth = (screenWidth)/(2.0*(hexCols+2));		//Split screen into x or y lengths with
		double unitThirdHeight   = (screenHeight)/(2.0*(hexRows+2));//one buffer strip on each edge
		//Not sure if unrolling is optimal or needed to prevent if statement.  Originally no helper functions!
		for(int row=0; row<hexRows; row+=2) {
			//Even Rows
			for(int col=0; col<hexCols; col++) {
				//Polygon format X[], Y[], number of polygons
				pixelColor = getHeightColor(row, col);
				g2d.setColor(pixelColor);
				int[][] edges = getEvenRowEdges(row, col, unitHalfWidth, unitThirdHeight);
				g2d.fillPolygon(edges[0], edges[1], 6);
				if (showGrid) {
					g2d.setColor(Color.BLACK);
					g2d.drawPolygon(edges[0], edges[1], 6);
				}
			}
			//Odd Rows
			for(int col=0; col<hexCols; col++) {
				pixelColor = getHeightColor(row+1, col);
				g2d.setColor(pixelColor);
				int[][] edges = getOddRowEdges(row+1, col, unitHalfWidth, unitThirdHeight);
				g2d.fillPolygon(edges[0], edges[1], 6);
				if (showGrid) {
					g2d.setColor(Color.BLACK);
					g2d.drawPolygon(edges[0], edges[1], 6);
				}
			}
		}
		
		//DRAW THE CITY TILES
		g2d.setStroke(new BasicStroke(2));
		g2d.setColor(Color.BLUE);
		for (CityHex curCity : infoService.getCities()) {
			int[][] adj = WorldOfChooChoo.getAdjacent(curCity.getRow(), curCity.getCol());
			for (int ii = 0; ii < 6; ii++) {
				if (adj[0][ii]%2==0) { //even rows
					int[][] edges = getEvenRowEdges(adj[0][ii], adj[1][ii], unitHalfWidth, unitThirdHeight);
					g2d.drawPolygon(edges[0], edges[1], 6);
				} else {
					int[][] edges = getOddRowEdges(adj[0][ii], adj[1][ii], unitHalfWidth, unitThirdHeight);
					g2d.drawPolygon(edges[0], edges[1], 6);
				}
			}
		}
		
		//DRAW PATHS
		int numCities = infoService.getNumCities();
		for (int curRowIndex = 0; curRowIndex < numCities; curRowIndex++) {
			ArrayList<CityPath> curRow = infoService.getAllPaths().get(curRowIndex);
			for (int curCol = curRowIndex+1; curCol < numCities; curCol++) {
				CityPath curPath = curRow.get(curCol);
				if (curPath != null) {
					g2d.setStroke(new BasicStroke(1));
					g2d.setColor(Color.CYAN);
					//Color randColor = new Color((float)Math.random(), (float)Math.random(), (float)Math.random());
					//g2d.setColor(randColor);
					ArrayList<Dimension> thisPath = curPath.getOptimalPath();
					for (int pathPos = 1; pathPos < thisPath.size(); pathPos++) {
						Dimension nextNode = thisPath.get(pathPos);
						Dimension prevNode = thisPath.get(pathPos-1);
						int[] nextNodeLoc = getCenter(nextNode.height, nextNode.width, unitHalfWidth, unitThirdHeight);
						int[] prevNodeLoc = getCenter(prevNode.height, prevNode.width, unitHalfWidth, unitThirdHeight);
						g2d.drawLine(nextNodeLoc[0], nextNodeLoc[1], prevNodeLoc[0], prevNodeLoc[1]);
					}
				}
			}
		}
		
		//DRAW EXPLORED REGIONS
		g2d.setColor(Color.RED);
		Dimension[] expandedNodes = infoService.getExpandedNodes();
		for (Dimension d : expandedNodes) {
			int[] locationOfDot = getCenter(d.height, d.width, unitHalfWidth, unitThirdHeight);
			g2d.drawOval(locationOfDot[0], locationOfDot[1], 2, 2);
		}
		
		/////New train section - actual dynamic elements of image
		// TODO cache the non changing parts if slow-down is occurring.
		g2d.setColor(Color.MAGENTA);
		g2d.drawOval((int) (500 * Math.random()), (int)( 500 * Math.random()), 5, 5);
		
		g2d.dispose();
	}
	
	//Probably can improve this
	//Too lazy at this moment to get directly
	public int[] getCenter(int row, int col,  double unitHalfWidth, double unitThirdHeight) {
		int[][] pos;
		if (row%2==0) {
			pos = getEvenRowEdges(row,col,unitHalfWidth,unitThirdHeight);
		} else {
			pos = getOddRowEdges(row,col,unitHalfWidth,unitThirdHeight);
		}
		return new int[] {(pos[0][2]+pos[0][5])/2, (pos[1][2]+pos[1][5])/2};
	}
	
	// Return int matrix corresponding to the vertices of a hexagon residing in the even rows
	// Note that some edges might be off by one due to rounding.
	public static int[][] getEvenRowEdges(int row, int col, double unitHalfWidth, double unitThirdHeight) {
		// Start with top, then right 2, then bottom, then left 2.						//    0
		double myLeft = (2*col+1)*unitHalfWidth;										//	5 /\ 1
		double myTop  = (2*row+1)*unitThirdHeight;										//   |  |
		// Polygon format X[], Y[], number of polygons									//	4 \/ 2
		return getOffsetVertFromTopLeft(myLeft, myTop, unitHalfWidth, unitThirdHeight);	//     3
	}

	
	// Return int matrix corresponding to the vertices of a hexagon residing in the odd rows
	// Note that some edges might be off by one due to rounding.
	public static int[][] getOddRowEdges(int row, int col, double unitHalfWidth, double unitThirdHeight) {
		// start with top, then right 2, then bottom, then left 2.						//    0
		double myLeft = (2*col+2)*unitHalfWidth;										//	5 /\ 1
		double myTop  = (2*row+1)*unitThirdHeight;										//   |  |
		// Polygon format X[], Y[], number of polygons									//	4 \/ 2
		return getOffsetVertFromTopLeft(myLeft, myTop, unitHalfWidth, unitThirdHeight);	//     3
	}
	
	public static int[][] getOffsetVertFromTopLeft (double myLeft, double myTop, double unitHalfWidth, double unitThirdHeight) {
		return new int[][]{
			{
				(int)(myLeft + unitHalfWidth), (int)(myLeft + 2*unitHalfWidth), (int)(myLeft + 2*unitHalfWidth),
				(int)(myLeft + unitHalfWidth), (int)(myLeft), 					(int)(myLeft)
			},{
				(int)(myTop),(int)(myTop+unitThirdHeight),(int)(myTop+2*unitThirdHeight),
				(int)(myTop+3*unitThirdHeight),(int)(myTop+2*unitThirdHeight),(int)(myTop+unitThirdHeight)
				}
			};
	}
	
	public void outputPNG() {
		try {
			ImageIO.write(theImage, "png", new File("Terrain"+screenWidth+"X"+screenHeight+".png"));
		} catch (IOException e) {}
	}
	
	private Color getHeightColor(int row, int col) {
		double[][] heightmap = infoService.getHeightmap();
		int[] range = infoService.getRange();
		for (int i = 0; i < terrainMax.length; i++) {
			if (range[i] <= heightmap[row][col] && heightmap[row][col] < range[i+1]) {
				if (discreteWithinTerrainTypes || terrainMax[i].equals(terrainMin[i])) {
					return terrainMin[i];
				} else {
					double gradient = (heightmap[row][col]-range[i])/(range[i+1]-range[i]);
					int red   = (int)(terrainMin[i].getRed()  +gradient*(terrainMax[i].getRed()  -terrainMin[i].getRed()));
					int green = (int)(terrainMin[i].getGreen()+gradient*(terrainMax[i].getGreen()-terrainMin[i].getGreen()));
					int blue  = (int)(terrainMin[i].getBlue() +gradient*(terrainMax[i].getBlue() -terrainMin[i].getBlue()));
					return new Color(red, green, blue);
				}
			}
		}
		return null; // Not possible
	}
	
	// Change the values of terrain check box settings
	public void setChkSettings(boolean nGrid, boolean nDiscrete) {
		showGrid = nGrid;
		discreteWithinTerrainTypes = nDiscrete;
		draw();
	}
	
	public void resizeCalculations(int newWidth, int newHeight) {
		screenWidth = newWidth;
		screenHeight = newHeight;
		theImage = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
	}
}