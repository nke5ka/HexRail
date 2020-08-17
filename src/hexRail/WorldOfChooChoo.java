package hexRail;

import java.awt.Dimension;
import java.util.ArrayList;

public class WorldOfChooChoo {
	// TERRAIN STUFF
	private int		hexRows	= 80;
	private int		hexCols	= 80;
	private double[][]	heightmap;
	//	DeepestOcean(Cutoff)			Beyond Deep Ocean		Ocean			Sand (ugly but use 30)			Grass					Mountain		BeyondSnowPeak
	private int[]	range 		= {Integer.MIN_VALUE,	-250, 					0, 					 0, 					300,					400,	Integer.MAX_VALUE};
	// Inputs for Map Gen
	private int		perturbs		= 170;
	private int		smoothings		= 5;
	
	// CITY STUFF
	private int numCities = 0;
	private int cityLower = 40;
	private int cityUpper = 200;
	ArrayList<CityHex> cities = new ArrayList<CityHex>();
	
	// ROUTES STUFF
	//True art is expandable matrix - sadness occurs because no generic [][]
	ArrayList<ArrayList<CityPath>> allPaths = new ArrayList<ArrayList<CityPath>>();
	Dimension[] expandedNodes = new Dimension[0];
	
	//TRAIN STUFF
	//Trains stored here
	ArrayList<Train> trains = new ArrayList<Train>();
	
	
	
	//Use diamond square algorithm to create fractal bitmap
	public void landGen() {
		//Destroy old cities.
		removeCities();
		expandedNodes = new Dimension[0];
		
		//Initialize corners to random values
		heightmap = new double[hexRows][hexCols];
		heightmap[0][0]					 = 300*Math.random()-100;
		heightmap[0][hexCols-1]			 = 300*Math.random()-100;
		heightmap[hexRows-1][0]			 = 300*Math.random()-100;
		heightmap[hexRows-1][hexCols-1] = 300*Math.random()-100;
		//Kai's variation of Midpoint Displacement
		horizontalLine(0,hexCols-1,0,1, perturbs);
		horizontalLine(0,hexCols-1,hexRows-1,1, perturbs);
		verticalLine(0,hexRows-1,0,1, perturbs);
		verticalLine(0,hexRows-1,hexCols-1,1, perturbs);
		cross(0,hexCols-1,0,hexRows-1,1, perturbs);
		for (int i = 0; i < smoothings; i++){
			gaussianBlur();
		}
	}
	
	public void foundCities(int numNewCities) {
		//cities = new CityHex[0];
		int oldSize = numCities;
		ArrayList<CityHex> candidateCity = new ArrayList<CityHex>();
		//Strive for somewhat random distribution with no clustering
		for (int preOffsetRow = 2; preOffsetRow < hexRows - 4; preOffsetRow+=4) {
			for (int col = (int)(2+2*Math.random()); col < hexCols - 2; col+=5+4*Math.random()) {
				int row = (int) (preOffsetRow+2*Math.random());
				//Check if within range of highest and lowest
				//Stricter to reduce likelihood of adjacent values off
				if (cityLower+5 < heightmap[row][col] && 
						heightmap[row][col] < cityUpper-5) {
					int[][] adj = getAdjacent(row,col);
					double cityScore = -6*heightmap[row][col];
					for (int ii = 0; ii < 6; ii++) {
						double adjH = heightmap[adj[0][ii]][adj[1][ii]];
						//disqualify adjacent slots out of bounds
						if (adjH > cityUpper || adjH < cityLower) {
							cityScore = 666666; //Marker so this is not added
							break;
						}
						cityScore += adjH;
					}
					if (cityScore != 666666) {
						//Checks candidate city in this round of generation against
						//already existent cities to prevent adjacency
						boolean tooClose = false;
						for (CityHex noTouchCheck : cities) {
							if (Math.abs(noTouchCheck.getRow() - row) < 4 && Math.abs(noTouchCheck.getCol() - col) < 3) {
								tooClose = true;
								break;
							}
						}
						if (!tooClose) {
							candidateCity.add(new CityHex(row, col, Math.abs(cityScore)));
						}
					}
				}
			}
		}
		// Choose best cities only!  Then change cities array to contents of ArrayList.
		// sort by the score (Comparable)
		int addCities = Math.min(numNewCities, candidateCity.size());
		numCities += addCities;
		candidateCity.sort(null);
		cities.addAll(candidateCity.subList(0, addCities));
		//numbering for the sake of the adjacency matrix
		for (int newCityIndex = oldSize; newCityIndex < numCities; newCityIndex++) {
			cities.get(newCityIndex).setId(newCityIndex);
		}
		
		// o x  Expand the edges of transport matrix
		// x x
		allPaths.ensureCapacity(numCities);
		for (ArrayList<CityPath> existingRow : allPaths) {
			for (int newCol = oldSize; newCol < numCities; newCol++) {
				existingRow.add(null);
			}
		}
		for (int newRowIndex = oldSize; newRowIndex < numCities; newRowIndex++) {
			ArrayList<CityPath> newRow = new ArrayList<CityPath>(numCities);
			for (int newCol = 0; newCol < numCities; newCol++) {
				newRow.add(null);
			}
			allPaths.add(newRow);
		}
		expandedNodes = new Dimension[0];
	}
	
	public void connectRandCities() {
		int city1 = (int)(numCities * Math.random());
		int city2 = (int)(numCities * Math.random());
		if (city1 != city2) {
			connect2Cities(Math.min(city1, city2), Math.max(city1, city2));
		}
	}
	
	// Upper Right Triangle of Connectedness
	// TODO Maybe make it upper left one day?
	public void connectAllCities() {
		for (int curRow = 0; curRow < numCities; curRow++) {
			for (int curCol = curRow+1; curCol < numCities; curCol++) {
				if (allPaths.get(curRow).get(curCol) == null) {
					connect2Cities(curRow, curCol);
				}
			}
		}
		expandedNodes = new Dimension[0];
	}
	
	public void connect2Cities(int rowIndex, int colIndex) {
		CityPath pathOf2 = new CityPath(heightmap, cities.get(rowIndex), cities.get(colIndex));
		allPaths.get(rowIndex).set(colIndex, pathOf2);
		expandedNodes = pathOf2.getExplored();
		//System.out.println(aRandPath.getOptimalPath().toString());
	}
	
	public void prunePaths() {
		// TODO Kill trains?
		allPaths = CityPath.kruskalMinForest(allPaths, cities);
	}
	
	
	
	// Destroy downwards through complexity levels
	
	public void removeCities() {
		cities.removeAll(cities);
		clearAllPaths();
		numCities = 0;
	}
	
	public void clearAllPaths() {
		killAllTrains();
		for (int curRow = 0; curRow < numCities; curRow++) {
			for (int curCol = curRow+1; curCol < numCities; curCol++) {
				allPaths.get(curRow).set(curCol, null);
			}
		}
	}
	
	public void killAllTrains() {
		
	}
	
	
	
	
	// Helper Algorithms
	
	//starting top left, to clockwise
	//Returns list of rows, and col
	//Does not check if off map!!!
	public static int[][] getAdjacent(int row, int col) {
		if (row%2 == 0) {
			//Even Rows
			return new int[][]{ {row-1,row-1,row,  row+1,row+1,row},
								{col-1,col,  col+1,col,  col-1,col-1}};
		} else {
			//Odd Rows
			return new int[][]{ {row-1,row-1,row,  row+1,row+1,row},
								{col  ,col+1,col+1,col+1,  col,col-1}};
		}
	}
	private void gaussianBlur() {
		double[][] blurredHeightmap = new double[hexRows][hexCols];
		//Central part
		for (int row = 1; row < hexRows-1; row++) {
			for (int col = 1; col < hexCols-1; col++) {
				blurredHeightmap[row][col] = (9*heightmap[row][col]+4*heightmap[row][col+1]+4*heightmap[row-1][col]+4*heightmap[row][col-1]+
						4*heightmap[row+1][col]+heightmap[row-1][col-1]+heightmap[row-1][col+1]+heightmap[row+1][col-1]+heightmap[row+1][col+1])/29;
			}
			
		}
		//Top part
		for (int col = 1; col < hexCols-1; col++) {
			blurredHeightmap[0][col] = (9*heightmap[0][col]+4*heightmap[0][col+1]+4*heightmap[0][col-1]
					+4*heightmap[1][col]+heightmap[1][col-1]+heightmap[1][col+1])/23;
		}
		//Bottom part
		for (int col = 1; col < hexCols-1; col++) {
			blurredHeightmap[hexRows-1][col] = (9*heightmap[hexRows-1][col]+4*heightmap[hexRows-1][col+1]
					+4*heightmap[hexRows-1][col-1]+4*heightmap[hexRows-2][col]+heightmap[1][col-1]+heightmap[hexRows-2][col+1])/23;
		}
		//Left part
		for (int row = 1; row < hexRows-1; row++) {
			blurredHeightmap[row][0] = (9*heightmap[row][0]+4*heightmap[row][1]+4*heightmap[row-1][0]
					+4*heightmap[row+1][0]+heightmap[row-1][1]+heightmap[row+1][1])/23;
		}
		//Right part
		for (int row = 1; row < hexRows-1; row++) {
			blurredHeightmap[row][hexCols-1] = (9*heightmap[row][hexCols-1]+4*heightmap[row][hexCols-2]+4*heightmap[row-1][hexCols-1]
					+4*heightmap[row+1][hexCols-1]+heightmap[row-1][hexCols-2]+heightmap[row+1][hexCols-2])/23;
		}
		//Corners
		blurredHeightmap[0][0] = heightmap[0][0];
		blurredHeightmap[0][hexCols-1] = heightmap[0][hexCols-1];
		blurredHeightmap[hexRows-1][0] = heightmap[hexRows-1][0];
		blurredHeightmap[hexRows-1][hexCols-1] = heightmap[hexRows-1][hexCols-1];
		heightmap = blurredHeightmap;
	}
	
	private void cross(int leftCol, int rightCol, int upRow, int downRow, int iteration, int perturbs) {
		if (leftCol+1 >= rightCol && upRow+1 >= downRow) { return; }
		horizontalLine(leftCol,rightCol, (upRow+downRow)/2, 	iteration, perturbs);
		verticalLine(	upRow,	downRow, (leftCol+rightCol)/2,	iteration, perturbs);
		cross((leftCol+rightCol)/2, rightCol, upRow, (upRow+downRow)/2, iteration+1, perturbs);		//NE Quadrant
		cross(leftCol, (leftCol+rightCol)/2, upRow, (upRow+downRow)/2, iteration+1, perturbs);		//NW Quadrant
		cross(leftCol, (leftCol+rightCol)/2, (upRow+downRow)/2, downRow, iteration+1, perturbs);		//SW Quadrant
		cross((leftCol+rightCol)/2, rightCol, (upRow+downRow)/2, downRow, iteration+1, perturbs);		//SE Quadrant
	}
	
	private void horizontalLine(int leftCol, int rightCol, int row, int iteration, int perturbs) {
		if (leftCol+1 >= rightCol) { return; }
		heightmap[row][leftCol+(rightCol-leftCol)/2] = (heightmap[row][leftCol]+heightmap[row][rightCol])/2 + perturbs/Math.sqrt(iteration)*(Math.random()-.5);
		horizontalLine(leftCol, leftCol+(rightCol-leftCol)/2, row, iteration, perturbs);
		horizontalLine(leftCol+(rightCol-leftCol)/2, rightCol, row, iteration, perturbs);
	}
	
	private void verticalLine(int upRow, int downRow, int col, int iteration, int perturbs) {
		if (upRow+1 >= downRow) { return; }
		heightmap[upRow+(downRow-upRow)/2][col] = (heightmap[upRow][col]+heightmap[downRow][col])/2 + perturbs*Math.sqrt(iteration)*(Math.random()-.5);
		verticalLine(upRow, upRow+(downRow-upRow)/2, col, iteration, perturbs);
		verticalLine(upRow+(downRow-upRow)/2, downRow, col, iteration, perturbs);
	}
	
	
	// All the Getters and Setters
	public int getHexRows() {
		return hexRows;
	}
	public void setHexRows(int hexRows) {
		this.hexRows = hexRows;
	}
	public int getHexCols() {
		return hexCols;
	}
	public void setHexCols(int hexCols) {
		this.hexCols = hexCols;
	}
	public int[] getRange() {
		return range;
	}
	public void setRange(int[] range) {
		this.range = range;
	}
	public int getNumCities() {
		return numCities;
	}
	public int getCityLower() {
		return cityLower;
	}
	public void setCityLower(int cityLower) {
		this.cityLower = cityLower;
	}
	public int getCityUpper() {
		return cityUpper;
	}
	public void setCityUpper(int cityUpper) {
		this.cityUpper = cityUpper;
	}

	public ArrayList<CityHex> getCities() {
		return cities;
	}

	public ArrayList<ArrayList<CityPath>> getAllPaths() {
		return allPaths;
	}

	public Dimension[] getExpandedNodes() {
		return expandedNodes;
	}

	public double[][] getHeightmap() {
		return heightmap;
	}

	public int getPerturbs() {
		return perturbs;
	}

	public void setPerturbs(int perturbs) {
		this.perturbs = perturbs;
	}

	public int getSmoothings() {
		return smoothings;
	}

	public void setSmoothings(int smoothings) {
		this.smoothings = smoothings;
	}

	public ArrayList<Train> getTrains() {
		return trains;
	}
}
