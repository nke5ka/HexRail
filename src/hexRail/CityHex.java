package hexRail;

public class CityHex implements Comparable<CityHex> {
	private int centerRow, centerCol, id;
	// Score represents desirability of city.  Currently modeled as
	// how flat these seven tiles are.
	private double score = -1;
	public CityHex(int locRow, int locCol, double nscore) {
		centerRow = locRow;
		centerCol = locCol;
		score	  = nscore;
	}
	@Override
	public int compareTo(CityHex o) {
		return (int) Math.signum(score - o.score);
	}
	public int getRow() {
		return centerRow;
	}
	public int getCol() {
		return centerCol;
	}
	public double getScore() {
		return score;
	}
	public void setId(int nid) {
		id = nid;
	}
	public int getId() {
		return id;
	}
}
