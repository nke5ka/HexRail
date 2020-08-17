package hexRail;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;


public class CityPath implements Comparable<CityPath> {
	//Create new path between two cities
	//City order shouldn't matter

	//don't need to store this?
	private double[][] heightmap;
	private CityHex city1, city2;
	private ArrayList<Dimension> optimalPath;
	private double totalCost;
	
	private Dimension[] explored = new Dimension[0];

	//Might want to add divisions between height-map.
	//Paradigm shift and total reorganization of coupling
	public CityPath(double[][] hmap, CityHex ncity1, CityHex ncity2) {
		heightmap = hmap;
		city1 = ncity1;
		city2 = ncity2;
		optimalPath = aStar();
	}
	
	public ArrayList<Dimension> getOptimalPath() {
		return optimalPath;
	}
	
	public double getTotalCost() {
		return totalCost;
	}
	
	public Dimension[] getExplored() {
		return explored;
	}

	private ArrayList<Dimension> aStar() {
		//Prepare data holding
		/////PROBABLY SHOULD USE SOMETHING OTHER THAN DIMENSION
		//For each tile, say where previous tile is
		Dimension[][] prevTile = new Dimension[heightmap.length][heightmap[0].length];
		//Cost of start node to node, initialize with infinity
		double[][] gScore = new double[heightmap.length][heightmap[0].length];
		for (double[] filledRow : gScore) {
			Arrays.fill(filledRow, Double.MAX_VALUE);
		}
		gScore[city1.getRow()][city1.getCol()] = 0;
		//Cost of traversed tiles + heuristic
		double[][] fScore = new double[heightmap.length][heightmap[0].length];
		for (double[] filledRow : fScore) {
			Arrays.fill(filledRow, Double.MAX_VALUE);
		}
		fScore[city1.getRow()][city1.getCol()]
				= estHeuristic(city1.getRow(), city1.getCol(), city2.getRow(), city2.getCol());

		//Prepare evaluation tools
		//Already evaluated - don't revisit
		HashSet<Dimension>		 closedTiles = new HashSet<Dimension>();
		//Tiles discovered, and awaiting evaluation - ordered so cheapest first
		//  Initial size is 256, and Comparator chooses cheapest f(x) = g(x) + h(x)
		PriorityQueue<Dimension> openTiles = new PriorityQueue<Dimension>(256,
				new Comparator<Dimension>() {
					@Override //Get lowest fScore
					public int compare(Dimension o1, Dimension o2) {
						return (int) -Math.signum(fScore[o2.height][o2.width]-fScore[o1.height][o1.width]);
					}
				}
			);
		openTiles.add(new Dimension(city1.getCol(), city1.getRow()));

		//Actual search can now begin
		while (openTiles.size() != 0) {
			Dimension current = openTiles.poll();
			//Check if done
			if (current.equals(new Dimension(city2.getCol(), city2.getRow()))) {
				//Generate the only path we care about
				ArrayList<Dimension> walkback = new ArrayList<Dimension>();
				Dimension currentOnWalk = new Dimension(city2.getCol(), city2.getRow());
				walkback.add(currentOnWalk);
				while (!currentOnWalk.equals(new Dimension(city1.getCol(), city1.getRow()))) {
					currentOnWalk = prevTile[currentOnWalk.height][currentOnWalk.width];
					walkback.add(currentOnWalk);
				}
				//Just to see where expanded
				explored = closedTiles.toArray(explored);
				totalCost = gScore[city2.getRow()][city2.getCol()];
				return walkback;
			}
			//Ensure we do not revisit
			closedTiles.add(current);
			//Traverse all the adjacent tiles
			int[][] adjTiles = WorldOfChooChoo.getAdjacent(current.height, current.width);
			for (int dir = 0; dir < 6; dir++) {
				//Check if out of bounds of map
				if (adjTiles[0][dir] < 0 || adjTiles[0][dir] >= heightmap.length
						|| adjTiles[1][dir] < 0 || adjTiles[1][dir] >= heightmap[0].length) {
					continue; //Skip this bad neighbor
				}
				//Check if we have already evaluated
				if (closedTiles.contains(new Dimension(adjTiles[1][dir], adjTiles[0][dir]))) {
					continue; //Skip what is already done
				}
				double thisGScore = gScore[current.height][current.width]
						+ getCostAdjTile(current.height,current.width,adjTiles[0][dir],adjTiles[1][dir]);
				//This adds neighbor if does not exist
				if (!openTiles.contains(new Dimension(adjTiles[1][dir], adjTiles[0][dir]))) {
					openTiles.add(new Dimension(adjTiles[1][dir], adjTiles[0][dir]));
				} else if (thisGScore >= gScore[adjTiles[0][dir]][adjTiles[1][dir]]) {
					//Worse result, don't change anything
					continue;
				}
				//Update costs and previous path
				prevTile[adjTiles[0][dir]][adjTiles[1][dir]] = current;
				gScore[adjTiles[0][dir]][adjTiles[1][dir]] = thisGScore;
				fScore[adjTiles[0][dir]][adjTiles[1][dir]] = thisGScore + estHeuristic(adjTiles[0][dir],adjTiles[1][dir], city2.getRow(), city2.getCol());
				//Need to tell the PriorityQueue to recalculate. This is horrendously inefficient
				openTiles.remove(new Dimension(adjTiles[1][dir],adjTiles[0][dir]));
				openTiles.add(new Dimension(adjTiles[1][dir],adjTiles[0][dir]));
			}
		}
		//You've failed me
		return null;
	}

	//Returns an estimation of cost * a constant
	//Constant = 0 - Dijkstra. 10 is good. All fast anyway.
	private double estHeuristic(int curRow, int curCol, int destRow, int destCol) {
		double HEURISTIC_CONSTANT = 7;
		return HEURISTIC_CONSTANT * Math.max(Math.abs(destRow-curRow),
				Math.abs((destCol + Math.floor(curCol/2)) - (curCol + Math.floor(destCol/2))));
		//Before "Chebyshev" distance, we had this
		//return 10*Math.hypot(destRow-curRow, destCol-curCol);
		
	}

	private double getCostAdjTile(int curRow, int curCol, int destRow, int destCol) {
		// Simple shortest path
		//return 1;
		// Avoid ups and down
		//return Math.abs(heightmap[curRow][curCol] - heightmap[destRow][destCol]);
		// Avoid ups and down, and also avoid water ("bridges")
		return .15+ Math.abs(heightmap[curRow][curCol] - heightmap[destRow][destCol])
				- Math.min(heightmap[destRow][destCol], 0)*20;
	}
	
	//Inefficient sets but it works - desire a disjoint set one day
	/*
	    KRUSKAL(G):
	1 A = NULL
	2 for each v elementOf G.V:
	3    MAKE-SET(v)
	4 for each (u, v) in G.E ordered by weight(u, v), increasing:
	5    if FIND-SET(u) != FIND-SET(v):
	6       A = A ===union=== {(u, v)}
	7       UNION(u, v)
	8 return A
	*/
	public static ArrayList<ArrayList<CityPath>> kruskalMinForest(ArrayList<ArrayList<CityPath>> allPaths, ArrayList<CityHex> cities) {
		//Create empty results holder
		ArrayList<ArrayList<CityPath>> selected = new ArrayList<ArrayList<CityPath>>();
		int numCities = cities.size();
		selected.ensureCapacity(numCities);
		for (int newRowIndex = 0; newRowIndex < numCities; newRowIndex++) {
			ArrayList<CityPath> newRow = new ArrayList<CityPath>(numCities);
			for (int newCol = 0; newCol < numCities; newCol++) {
				newRow.add(null);
			}
			selected.add(newRow);
		}
		
		//Create sets to represent forests -- Would trees be more KruskalEsque?
		ArrayList<HashSet<CityHex>> sets = new ArrayList<HashSet<CityHex>>(numCities);
		for (CityHex city : cities) {
			HashSet<CityHex> oneCityHash = new HashSet<CityHex>();
			oneCityHash.add(city);
			sets.add(oneCityHash);
		}
		
		//Create an ordered list of paths
		ArrayList<CityPath> pathsToAdd = new ArrayList<CityPath>(numCities*numCities/2);
		for (int curRowIndex = 0; curRowIndex < numCities; curRowIndex++) {
			ArrayList<CityPath> curRow = allPaths.get(curRowIndex);
			for (int curCol = curRowIndex+1; curCol < numCities; curCol++) {
				CityPath curPath = curRow.get(curCol);
				if (curPath != null) {
					pathsToAdd.add(curPath);
				}
			}
		}
		pathsToAdd.sort(null);
		
		//Traverse the ordered list and add iff it increases connectivity
		for (CityPath toAdd : pathsToAdd) {
			CityHex city1 = toAdd.city1;
			CityHex city2 = toAdd.city2;
			HashSet<CityHex> setFor1 = null;
			HashSet<CityHex> setFor2 = null;
			for (HashSet<CityHex> checkSet1 : sets) {
				if (checkSet1.contains(city1)) {
					setFor1 = checkSet1;
					break;
				}
			}
			for (HashSet<CityHex> checkSet2 : sets) {
				if (checkSet2.contains(city2)) {
					setFor2 = checkSet2;
					break;
				}
			}
			if (setFor1 != setFor2) {
				setFor1.addAll(setFor2);
				sets.remove(setFor2);
				selected.get(city1.getId()).set(city2.getId(), toAdd);
			}
		}
		
		//Done!
		return selected;
	}

	@Override
	public int compareTo(CityPath other) {
		return (int)Math.signum(totalCost - other.totalCost);
	}
}
