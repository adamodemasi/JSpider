package src;

public class PredMove {

	int colDest;
	int rigaDest;
	
	public PredMove(int a, int b) {

		setColDest(a);
		setRigaDest(b);
	}

	public int getColDest() {
		return colDest;
	}

	public void setColDest(int colDest) {
		this.colDest = colDest;
	}

	public int getRigaDest() {
		return rigaDest;
	}

	public void setRigaDest(int rigaDest) {
		this.rigaDest = rigaDest;
	}
	
	@Override
	public String toString() {
		return "predMove("+getColDest()+","+getRigaDest()+").\r\n";
	}
}
