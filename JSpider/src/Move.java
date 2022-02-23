package src;

import it.unical.mat.embasp.languages.Id;
import it.unical.mat.embasp.languages.Param;

@Id("move")
public class Move {

	@Param(0)
	private int colPart;
	@Param(1)
	private int rigaPart;
	@Param(2)
	private int colDest;
	@Param(3)
	private int rigaDest;
	
	public Move() {

	}

	public Move(int col, int row, int moveCol, int moveRow) {
		super();
		this.colPart = col;
		this.rigaPart = row;
		this.colDest = moveCol;
		this.rigaDest = moveRow;
	}

	@Override
	public String toString() {
		return "move(" + getColPart() + "," + getRigaPart() + "," + getColDest() + "," + getRigaDest() + ")";

	}

	public int getColPart() {
		return colPart;
	}

	public void setColPart(int colPart) {
		this.colPart = colPart;
	}

	public int getRigaPart() {
		return rigaPart;
	}

	public void setRigaPart(int rigaPart) {
		this.rigaPart = rigaPart;
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

}
