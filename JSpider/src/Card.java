package src;

import it.unical.mat.embasp.languages.Id;
import it.unical.mat.embasp.languages.Param;

@Id("card")
public class Card {

	@Param(0)
	private int col;
	@Param(1)
	private int row;
	@Param(2)
	private int rank;
	@Param(3)
	private int suit;

	public Card() {

	}

	public Card(int col, int row, int rank, int suit) {
		super();
		this.col = col;
		this.row = row;
		this.rank = rank;
		this.suit = suit;
	}

	@Override
	public String toString() {
		return "card(" + getCol() + "," + getRow() + "," + getRank() + "," + getSuit() + ")";

	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getSuit() {
		return suit;
	}

	public void setSuit(int suit) {
		this.suit = suit;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

}
