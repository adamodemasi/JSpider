package src;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import it.unical.mat.embasp.base.InputProgram;
import it.unical.mat.embasp.base.Output;
import it.unical.mat.embasp.languages.IllegalAnnotationException;
import it.unical.mat.embasp.languages.ObjectNotValidException;
import it.unical.mat.embasp.languages.asp.ASPInputProgram;
import it.unical.mat.embasp.languages.asp.ASPMapper;
import it.unical.mat.embasp.languages.asp.AnswerSet;
import it.unical.mat.embasp.languages.asp.AnswerSets;
import it.unical.mat.embasp.platforms.desktop.DesktopHandler;
import it.unical.mat.embasp.specializations.dlv.desktop.DLVDesktopService;

@SuppressWarnings("serial")
public class JSpider extends JFrame implements ActionListener, ComponentListener, WindowListener, Runnable {
	private int width = 1100;
	private int height = 700;

	private ImageIcon icon;

	// DLV Classes
	private DesktopHandler handler;
	private InputProgram inputProgram;
	private File file;

	private List<PredMove> predMove;

	private JMenu menu;
	private JMenuBar menuBar;
	private JMenuItem newGame;
	private JMenuItem changeDifficulty;
	private JMenuItem exit;

	private JPanel panel = new JPanel();
	private JButton plButton = new JButton("PLAY");
	private JButton uButton = new JButton("UNDO");
	private JButton paButton = new JButton("PAUSE");
	private boolean suspended = false;

	private GameBoard board = null;

	{
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		}
	}

	public JSpider() throws ObjectNotValidException, IllegalAnnotationException {
		super("JSpider");

		// DLV SET
		handler = new DesktopHandler(new DLVDesktopService("dlv.exe"));

		inputProgram = new ASPInputProgram();
		inputProgram.addFilesPath("rules.dl");
		handler.addProgram(inputProgram);
		file = new File("rules.dl");

		predMove = new ArrayList<PredMove>();

		// Registra le classi di DLV
		registerClass();

		setMinimumSize(new Dimension(width, height));
		setLocationRelativeTo(null);

		menuBar = new JMenuBar();

		menu = new JMenu("Menu");

		newGame = new JMenuItem("New game");
		changeDifficulty = new JMenuItem("Change difficulty");
		exit = new JMenuItem("Exit");

		newGame.addActionListener(this);
		changeDifficulty.addActionListener(this);
		exit.addActionListener(this);

		menu.add(newGame);
		menu.add(changeDifficulty);
		menu.add(exit);

		menuBar.add(menu);

		paButton.setSize(60, 40);
		plButton.setSize(60, 40);
		uButton.setSize(60, 40);
		paButton.setText("PAUSE");
		plButton.setText("PLAY");
		uButton.setText("UNDO");
		paButton.setVisible(true);
		paButton.setBounds(5, 5, 65, 45);
		plButton.setBounds(5, 60, 65, 45);
		uButton.setBounds(50, 30, 65, 45);

		// PAUSE BUTTON
		paButton.setEnabled(true);
		paButton.addActionListener(new ActionListener() {
			@Override
			public synchronized void actionPerformed(ActionEvent e) {
				suspended = true;
				uButton.setVisible(true);
				
				if(board.canUndo())
					board.undoStack.pop();
			}
		});

		// PLAY BUTTON
		plButton.setEnabled(true);
		plButton.addActionListener(new ActionListener() {
			@Override
			public synchronized void actionPerformed(ActionEvent e) {
				repaint();
				suspended = false;
				uButton.setVisible(false);
			}
		});

		// UNDO BUTTON
		uButton.setEnabled(true);
		uButton.addActionListener(new ActionListener() {
			@Override
			public synchronized void actionPerformed(ActionEvent e) {
				if (board.canUndo()) {
					board.undo();
					repaint();
				}
				else uButton.setVisible(false);
			}
		});

		panel.add(paButton);
		panel.add(plButton);
		panel.add(uButton);
		panel.setBounds(675, 560, 160, 90);
		panel.setOpaque(false);
		panel.setFocusable(true);

		setJMenuBar(menuBar);

		selectDifficulty();

		setContentPane(board);
		board.setInsets(getInsets());
		getContentPane().add(panel);
		icon = new ImageIcon(readImage("/res/icon.png"));
		setIconImage(icon.getImage());
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		addComponentListener(this);
		addWindowListener(this);
		setVisible(true);

	}

	// Registra le classi di DLV
	private void registerClass() throws ObjectNotValidException, IllegalAnnotationException {

		ASPMapper.getInstance().registerClass(Card.class);
		ASPMapper.getInstance().registerClass(Move.class);

	}

	private boolean colonnaVuota() {
		for (int i = 0; i < board.pile.length; i++)
			if (board.pile[i].isEmpty())
				return true;
		return false;
	}

	@Override
	public void run() {
		suspended = false;
		try {
			gamePlay();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void gamePause() throws InterruptedException {
		while (suspended)
			Thread.sleep(200);
	}

	public synchronized void gamePlay() throws InterruptedException {

		while (true) {
			gamePause();
			// elimino il file creato in precedenza
			file.delete();

			Writer output;

			// Aggiorno il file rules.dl
			try {
				output = new BufferedWriter(new FileWriter("rules.dl", true));
				output.write("%FATTI\r\n");
				// aggiungo i nuovi fatti
				for (int i = 0; i < board.pile.length; i++) {
					for (int j = 0; j < board.pile[i].size(); j++) {

						Cards c = board.pile[i].get(j);

						if (!c.isFaceDown)
							output.append("card(" + i + "," + j + "," + c.toString() + ")." + "\r\n");
					}
				}

				for (PredMove p : predMove)
					output.append(p.toString());

				// Appendo le regole
				String rules = "%REGOLE ...................\n"
					+ "move(ColP,RigaP,ColD,RigaD)|noMove(ColP,RigaP,ColD,RigaD):-possibleMove(ColP,RigaP,ColD,RigaD).\r\n"
					+ ":-move(_,0,ColD,0),colSize(ColD,0).\r\n"
					+ ":-predMove(ColP,RigaP),move(ColP,RigaP,_,_).\r\n"
// 1 mossa
					+ ":-not #count{ColP,RigaP,ColD,RigaD:move(ColP,RigaP,ColD,RigaD)}=1.\r\n"
// mossa tra carte stesso seme
					+ ":~noMove(ColP,RigaP,ColD,RigaD),card(ColP,RigaP,_,Suit),card(ColD,RigaD,_,Suit). [1:15]\r\n"

//	+ "%-----1) favorire le scale più lunghe\r\n"
					+ ":~noMove(ColP,RigaP,ColD,_),dimScala(ColP,RigaP,SizeP),dimScala(ColD,_,Size1),Add=SizeP-RigaP, NewSize=Size1+Add. [NewSize:7]\r\n"
//	+ "%-----X) se una scala mi blocca carte da flippare, spostala\r\n"
					+ ":~noMove(ColP,RigaP,ColD,RigaD),colSize(ColP,Size),dimScala(ColP,RigaP,Dim),Size>Dim,X=Size-Dim,X>0. [1:6]\r\n"
					/* se in colonna c'è solo una carta scoperta, cerca di spostarla */
					+ ":~noMove(ColP,RigaP,_,_),dimScala(ColP,RigaP,1). [1:5]\r\n"
//	+ "%-----2) spostare le carte più alte\r\n"
					+ ":~noMove(ColP,_,_,_),card(ColP,_,Rank,_). [Rank:4]\r\n"

//	+ "%-----3) spostare la carta che libera la carta più alta\r\n"
					+ ":~noMove(ColP,RigaP,_,_),card(ColP,Row,Rank,_),dimScala(ColP,_,1), Row=RigaP-1. [Rank:3]\r\n"

//	+ "%---- 4) se sposto una carta sotto un'altra, fa' che sia la più alta possibile
					+ ":~noMove(ColP,RigaP,ColD,RigaD),dimScala(ColP,_,1),card(ColP,RigaP,Rank,_). [Rank:2]\r\n";

				String rules1 = "%REGOLE 1...................\n"
//	+ "%------ non fare una mossa che non crei una scala più lunga"
					+ ":-move(ColP,RigaP,ColD,_),dimScala(ColP,_,Size),dimScala(ColD,_,Size1),colSize(ColP,SizeP), "
					+ "Add=SizeP-RigaP, New=Size1+Add, Size>=New.\n";

				String rules2 = "%REGOLE 2...................\n"
//	+ "%------ non fare una mossa che non crei una scala più lunga"
					+ ":-move(ColP,RigaP,ColD,_),dimScala(ColP,_,Size),dimScala(ColD,_,Size1),colSize(ColP,SizeP),colSize(ColD,SizeD),"
					+ "SizeD<>0,Add=SizeP-RigaP, New=Size1+Add, Size>=New.\r\n";

				String rulesEmptyCol = "%REGOLE COLONNA VUOTA...................\n"
//	+ "%------ non spostare delle carte verso una colonna vuota se a sua volta svuoterebbe la colonna di partenza"
					+ ":-move(_,0,ColD,0),colSize(ColD,0).\r\n"
					+ ":-not #count{ColP,RigaP,ColD:move(ColP,RigaP,ColD,0)}=1.\r\n"
// + "%voglio subito una mossa\r\n"
					+ ":~noMove(ColP,RigaP,ColD,0). [1:14]\r\n"
// + "%spostare il Re, o una scala che inizia dal Re, o la carta più bassa, o la scala più lunga\r\n"
/* RE */ 			+ ":~noMove(ColP,RigaP,ColD,0),card(ColP,RigaP,13,_). [1:13]\r\n"
					/* scala che finisce con un ASSO */
//TODO
					+ ":~noMove(ColP,_,ColD,0),dimScala(ColP,_,Dim),card(ColP,Row,1,_),colSize(ColP,Size),Row=Size-1, Dim>1. [1:12]\r\n"
					/* scala dalla colonna con più carte */
					+ ":~noMove(ColP,RigaP,ColD,0),colSize(ColP,Size),dimScala(ColP,RigaP,_). [Size:11]\r\n"
//	/* ASSO */	+ ":~noMove(ColP,RigaP,ColD,0),card(ColP,RigaP,1,_). [1:10]\r\n"
					/* carta più bassa che non sia in scala */
					+ ":~move(ColP,RigaP,ColD,0),card(ColP,RigaP,Rank,_),dimScala(ColP,_,1). [Rank:9]\r\n"
					/* scala che inizia dalla carta più alta */
					+ ":~noMove(ColP,RigaP,ColD,0),dimScala(ColP,RigaP,Size),card(ColP,RigaP,Rank,_), Size>1. [Rank:8]\r\n"
					/* scala più lunga */
					+ ":~noMove(ColP,RigaP,ColD,0),dimScala(ColP,RigaP,Size). [Size:1]\r\n";

				output.append(carteChePossoMuovere());
				output.append(calcolaDimensioneColonne());
				output.append(calcolaDimensioneScale());
				output.append(rules);

				if (!colonnaVuota()) {
					if (board.cont < 6)
						output.append(rules1);
					else
						output.append(rules2);
				}
				
				if(colonnaVuota()) {
					if(board.cont < 6)
						output.append(rulesEmptyCol);
				}
				
				output.close();

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			// Lancio dlv
			Output o = handler.startSync();

			// Controlli
			AnswerSets answerSets = (AnswerSets) o;

			if (!answerSets.getAnswersets().isEmpty()) {

				AnswerSet as = answerSets.getAnswersets().get(0);
//System.out.println(as.toString());
				try {
					for (Object obj : as.getAtoms()) {

						// Se c'è una mossa la eseguo
						if (obj instanceof Move) {

							Move move = (Move) obj;
//System.out.println("ESEGUO LA MOSSA " + move.toString());
							int col = move.getColPart();
							int row = move.getRigaPart();
							int colDest = move.getColDest();
							int rigaDest = move.getRigaDest();

							if (board.cont < 7) {
								// salvo la mossa per evitare loop
								PredMove pm = new PredMove(0, 0);

								if (board.pile[colDest].isEmpty())
									pm = new PredMove(colDest, 0);
								else
									pm = new PredMove(colDest, rigaDest + 1);
								predMove.add(pm);

								if (!predMove.isEmpty())
									for (int i = 0; i < predMove.size(); i++) {
										PredMove p = predMove.get(i);
										if (!p.equals(pm))
											if (p.getColDest() == pm.getColDest()) {
												predMove.remove(p);
												continue;
											}
									}
							} else
								predMove.clear();

							int size = board.pile[col].size();

							// se la carta è la cima di una scala, le devo spostare tutte
							if (row < size - 1) {

								List<Cards> straight = board.pile[col].subList(row, size).stream()
										.collect(Collectors.toCollection(ArrayList::new));
								board.pile[colDest].addAll(board.pile[colDest].size(), straight);
//System.out.println("SPOSTO SCALA,LA COLONNA "+colDest+" DIVENTA "+board.pile[colDest].toString());
								// elimino la scala spostata dalla colonna di provenienza
								List<Cards> replace = board.pile[col].subList(0, row).stream()
										.collect(Collectors.toCollection(ArrayList::new));
								board.pile[col].clear();
								board.pile[col].addAll(replace);
								straight.clear();
								replace.clear();
								board.fixPile(colDest);
								board.fixPile(col);
								repaint();
							} else if (row == size - 1) {

								board.pile[colDest].add(board.pile[colDest].size(), board.pile[col].get(row));
//System.out.println("SPOSTO CARTA " + board.pile[col].get(row).toString() + " da " +col+ "," + row + " IN " + colDest + "," + rigaDest);
								board.pile[col].remove(row);
								board.fixPile(colDest);
								board.fixPile(col);
								repaint();
							}

							if (board.checkForCardsToRemove(colDest)) {
								board.score += 100;
								repaint();
								if (board.allCards.size() == 104) {
									repaint();
									board.showPlayAgainDialog();
								}
								predMove.clear();
								board.fixPile(col);
								checkForFlip();
								board.undoStack.push(new GameState(board.allCards, board.pile, board.deck, board.top, board.ptr));
								repaint();
								continue;
							} else {
								board.score--;
								board.moves++;
							}

							board.fixPile(col);

//							try {
//								Thread.sleep(750);
//							} catch (InterruptedException ex) {
//								Thread.currentThread().interrupt();
//							}
							checkForFlip();
							board.undoStack.push(new GameState(board.allCards, board.pile, board.deck, board.top, board.ptr));

							repaint();
							break;
						}
					}
				} catch (Exception e) {
				}
			} // chiusura if answerset vuoto se non ci sono mosse ma ancora carte da distribuire
			else if (!colonnaVuota() && board.top >= 0) {

//				try {
//					Thread.sleep(250);
//				} catch (InterruptedException ex) {
//					Thread.currentThread().interrupt();
//				}
				JOptionPane.showMessageDialog(null, "Mosse terminate,\ndistribuisco nuove carte");
				board.deal();
				predMove.clear();

				repaint();

				try {
					Thread.sleep(700);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				continue;

			} else { // se non ci sono più carte da distribuire e mosse da fare, game over
				for (int i = 0; i < board.pile.length; i++) {
					if (board.checkForCardsToRemove(i)) {
						board.score += 100;
						if (board.allCards.size() == 104) {
							repaint();
							board.showPlayAgainDialog();
						}
					}
				}
				board.showPlayAgainNoWin();
			}

			checkForFlip();
			repaint();
			try {
				Thread.sleep(850);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

		}

	}

	public void checkForFlip() {

		for (int i = 0; i < board.pile.length; i++) {

			if (board.pile[i].isEmpty())
				continue;

			Cards c = board.pile[i].get(board.pile[i].size() - 1);

			if (c.isFaceDown()) {
				c.flip();
				predMove.clear();
			}
		}
	}

	public synchronized String calcolaDimensioneColonne() {

		StringBuilder builder = new StringBuilder();
		List<String> dimension = new ArrayList<String>();
		List<Cards>[] actualPile = board.pile;

		for (int i = 0; i < actualPile.length; i++) {
			if (actualPile[i].isEmpty())
				dimension.add("colSize(" + i + ",0)." + "\r\n");
			else
				dimension.add("colSize(" + i + "," + actualPile[i].size() + ")." + "\r\n");
		}

		for (String str : dimension)
			builder.append(str);

		return builder.toString();
	}

	public synchronized String calcolaDimensioneScale() {
		List<String> dimScala = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		List<Cards>[] actualPile = board.pile;

		for (int i = 0; i < actualPile.length; i++) {
			int last = actualPile[i].size() - 1;
			boolean flag = false;

			if (actualPile[i].isEmpty()) {
				dimScala.add("dimScala(" + i + ",0,0).\r\n");
				continue;
			}
			if (last == 0) {
				dimScala.add("dimScala(" + i + "," + "0,1).\r\n");
				continue;
			}
			Cards c = actualPile[i].get(last);

			int rank = c.getRank() + 1;

			for (int k = last - 1; k >= 0 && !flag; k--, rank++) {
				flag = false;
				int dim = last - k;
				Cards c1 = actualPile[i].get(k);

				if (!c1.isFaceDown) {

					if (c1.getRank() == rank) {
						if (c1.getSuit() != c.getSuit()) {
							flag = true;
						}

						if (k == 0) {
							dimScala.add("dimScala(" + i + "," + "0," + (dim + 1) + ").\r\n");
						}
					} else {
						// salva lunghezza scala e coordinata ultima carta
						dimScala.add("dimScala(" + i + "," + (k + 1) + "," + dim + ").\r\n");
						flag = true;
					}
				} else {
					dimScala.add("dimScala(" + i + "," + (k + 1) + "," + dim + ").\r\n");
					flag = true;
				}
			}
		}

		for (String str : dimScala)
			builder.append(str);
		return builder.toString();
	}

	// trova tutte le carte che potrebbero essere spostate
	public String carteChePossoMuovere() {

		List<String> moveList = new ArrayList<>();
		List<String> dimScala = new ArrayList<>();
		List<Cards>[] actualPile = board.pile;
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < actualPile.length; i++) {

			boolean flag = false;

			if (actualPile[i].isEmpty())
				continue;

			int last = actualPile[i].size() - 1;
			possibleMoves(actualPile[i].get(last), i, last, moveList); // ultima carta della colonna, potrebbe sempre
																		// spostarsi

			for (int j = last; j >= 0 && !flag; j--) {
				Cards c = actualPile[i].get(j);

				if (!c.isFaceDown) {
					int rank = c.getRank() + 1;

					for (int k = j - 1; k >= 0; k--, rank++) {
						Cards c1 = actualPile[i].get(k);
						if (!c1.isFaceDown) {
							if (c1.getRank() == rank) {
								if (c1.getSuit() == c.getSuit()) {
									possibleMoves(c1, i, k, moveList);
								} else {
									flag = true;
									break;
								}
							} else {
								int numero = j - k;
								// salva lunghezza scala e coordinata ultima carta
								dimScala.add("dimScala(" + i + "," + j + "," + numero + ")." + "\r\n");

								flag = true;
								break;
							}
						} else {
							flag = true;
							break;
						}
					}
				} else {
					flag = true;
					break;
				}
			}
		}

		for (String str : moveList)
			builder.append(str);

		return builder.toString();
	}

	// calcola le destinazioni possibili di una carta che può essere spostata
	private synchronized void possibleMoves(Cards c1, int col, int row, List<String> moveList) {

		String actMove;
		List<Cards>[] actualPile = board.pile;

		for (int i = 0; i < actualPile.length; i++) {
			if (i != col) { // evito di controllare la stessa colonna

				if (actualPile[i].isEmpty()) {
					actMove = "possibleMove(" + col + "," + row + "," + i + ",0)." + "\r\n";

					if (!moveList.contains(actMove))
						moveList.add("possibleMove(" + col + "," + row + "," + i + ",0)." + "\r\n");
					continue;
				}

				int j = actualPile[i].size() - 1; // indice dell'ultima carta della colonna
				Cards c2 = actualPile[i].get(j);
				int vecchia = c1.getRank();
				int nuova = c2.getRank();

				// se il valore della carta che ho trovato è maggiore di 1, è una destinazione
				// possibile
				if (nuova - vecchia == 1) {
					actMove = "possibleMove(" + col + "," + row + "," + i + ",0)." + "\r\n";
					if (!moveList.contains(actMove))
						moveList.add("possibleMove(" + col + "," + row + "," + i + "," + j + ")." + "\r\n");
				} else
					continue;
			}
		}
	}

	private Image readImage(String fileName) {
		Image img = null;

		try {
			img = ImageIO.read(getClass().getResource(fileName));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return img;
	}

	private void selectDifficulty() {
		String[] difficulties = { "Easy", "Medium", "Hard" };
		String choice = (String) JOptionPane.showInputDialog(this, "Select difficulty:", "New game",
				JOptionPane.QUESTION_MESSAGE, null, difficulties, difficulties[0]);

		if (board == null && choice == null) {
			board = new GameBoard("Easy"); // se non c'è una scelta, liv facile è default
		} else if (board == null && choice != null) {
			board = new GameBoard(choice);
		} else if (choice != null) {
			board.clearCards();
			board.loadImages(choice);
			board.newGame();
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == newGame) {
			board.newGame();
		} else if (source == changeDifficulty) {
			selectDifficulty();
		} else {
			System.exit(0);
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		width = e.getComponent().getWidth();
		height = e.getComponent().getHeight();

		board.calcYCutoff();
		board.fixPiles();
		board.fixDeck();
		board.fixJunk();
		board.repaint();
	}

	@Override
	public void windowClosing(WindowEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	private class GameBoard extends JComponent {
		private final int piles = 10;
		private final int slots = 6; // deck slots, each slot have 10 (piles) cards

		private final int margin = 10;

		private final int cardWidth = 71;
		private final int cardHeight = 96;

		private final Color bgColor = new Color(0, 120, 0);

		private List<Cards> allCards; // also it's a junk pile

		private Image cardBack;

		private List<Cards>[] pile;
		private List<Cards>[] deck;

		private int top; // pointer to top
		private int ptr;

		private int yCutoff;

		private int score;
		private int moves;
		private int cont = 0;

		private List<Cards> movingPile;

		private Stack<GameState> undoStack;

		@SuppressWarnings("unused")
		private String difficulty;
		private Insets insets;

		public GameBoard(String difficulty) {
			cardBack = readImage("/res/back.png");
			insets = new Insets(0, 0, 0, 0);
			suspended = false;
			undoStack = new Stack<>();

			loadImages(difficulty);
			calcYCutoff();
			newGame();

		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			g.setColor(bgColor);
			g.fillRect(0, 0, width, height);

			int xGap = (width - piles * cardWidth) / (piles + 1);
			int y = margin;

			for (int i = 0; i < piles; i++) {
				int x = xGap + i * cardWidth + i * xGap - insets.left;

				g.setColor(Color.WHITE);
				g.fillRect(x, y, cardWidth, cardHeight); // un rettangolo bianco per indicare che non c'è carta
				g.setColor(bgColor);
				g.fillRect(x + 1, y + 1, cardWidth - 2, cardHeight - 2);

				pile[i].stream().forEach(card -> {
					g.drawImage(card.isFaceDown() ? cardBack : card.getImage(), card.x, card.y, this);
				});
			}

			for (int i = 0; i <= top; i++) {
				Cards card = deck[i].get(0);
				g.drawImage(cardBack, card.x, card.y, this);
			}

			for (int i = 12; i <= ptr; i += 13) {
				Cards card = allCards.get(i);
				g.drawImage(card.getImage(), card.x, card.y, this);
			}

			g.setColor(bgColor.darker());
			g.fillRect(width / 2 - 100 - insets.left, height - cardHeight - margin - insets.bottom - 40, 200,
					cardHeight);

			StringBuilder sb = new StringBuilder();
			sb.append("Score: ").append(score);
			sb.append("\nMoves: ").append(moves);

			g.setColor(Color.WHITE);
			g.setFont(new Font("consolas", Font.BOLD, 14));
			drawString(g, sb.toString(), width / 2 - 50, height - cardHeight - margin - insets.bottom - 50 + 35);

			if (movingPile != null) {
				movingPile.stream().forEach(card -> {
					g.drawImage(card.getImage(), card.x, card.y, this);
				});
			}
		}

		public void setInsets(Insets insets) {
			this.insets = insets;
			repaint();
		}

		public boolean canUndo() {
			return !undoStack.empty() && !undoStack.peek().dealed();
		}

		public void undo() {
			GameState state = undoStack.pop();

			allCards = state.getAllCards();
			pile = state.getPile();
			top = state.getTop();
			ptr = state.getPtr();

			repaint();

			score--;
			moves++;

		}

		public void clearCards() {
			collectAllCards();
			allCards = null;
		}

		// This method extends of Graphics::drawString, it handles strings with newlines
		private void drawString(Graphics g, String str, int x, int y) {
			for (String line : str.split("\n"))
				g.drawString(line, x, y += g.getFontMetrics().getHeight());
		}

		private void deal() {

			undoStack.clear();

			for (int i = 0; i < piles; i++) {
				Cards card = deck[top].get(i);
				pile[i].add(card);
				card.flip();
				fixPile(i);
			}

			deck[top--] = null;
			uButton.setVisible(false);

			repaint();
		}

		public void calcYCutoff() {
			yCutoff = height * 3 / 5;
		}

		public void fixJunk() {
			int y = height - cardHeight - margin - 40 - insets.bottom - 16;

			for (int i = 0; i <= ptr; i++) {
				Cards card = allCards.get(i);

				card.y = y;
			}
		}

		public void fixDeck() {
			int y = height - cardHeight - margin - 40 - insets.bottom - 16;

			for (int i = 0; i <= top; i++) {
				int x = width - cardWidth - margin - insets.left - 10 - (margin + 2) * i;

				deck[i].stream().forEach(card -> {
					card.x = x;
					card.y = y;
				});
			}
		}

		public void fixPiles() {
			for (int i = 0; i < piles; i++)
				fixPile(i);
		}

		private void fixPile(int index) {
			if (pile[index].size() == 0)
				return;

			int xGap = (width - piles * cardWidth) / (piles + 1);
			int topX = xGap + index * cardWidth + index * xGap - insets.left;
			int yGap = 35;

			for (int i = 0; i < 6; i++) {
				int cards = pile[index].size();

				Cards prevCard = null;

				yGap -= (i < 4) ? 4 : 1;

				for (int j = 0; j < cards; j++) {
					Cards card = pile[index].get(j);

					int topY = (prevCard == null) ? margin : prevCard.y + (prevCard.isFaceDown() ? margin : yGap);

					card.x = topX;
					card.y = topY;

					prevCard = card;
				}

				int lastY = pile[index].get(pile[index].size() - 1).y;

				if (lastY < yCutoff)
					break;
			}
		}

		private void collectAllCards() {
			if (allCards.size() < 104) {
				for (int i = 0; i < piles; i++) {
					int cards = pile[i].size();

					for (int j = 0; j < cards; j++) {
						Cards card = pile[i].get(j);

						if (!card.isFaceDown())
							card.flip();

						allCards.add(card);
					}
				}

				for (int i = 0; i <= top; i++) {
					List<Cards> slot = deck[i];

					int size = slot.size();

					for (int j = 0; j < size; j++)
						allCards.add(slot.get(j));

				}
			}
		}

		public void newGame() {
			collectAllCards();
			Collections.shuffle(allCards);
			initDeck();
			initPiles();
			deal();
			undoStack.clear();
			uButton.setVisible(false);

			ptr = -1;
			score = 500;
			moves = 0;
			cont = 0;
			movingPile = null;
		}

		@SuppressWarnings("unchecked")
		private void initDeck() {
			deck = new List[slots];

			top = slots - 1;

			int y = height - cardHeight - margin - 40 - insets.bottom - 16;

			for (int i = 0; i < slots; i++) {
				deck[i] = new ArrayList<>();

				int x = width - cardWidth - margin - insets.left - 10 - (margin + 2) * i;

				for (int j = 0; j < piles; j++) {
					Cards card = draw();

					card.x = x;
					card.y = y;
					card.width = cardWidth;
					card.height = cardHeight;

					deck[i].add(card);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private void initPiles() {
			pile = new List[piles];

			for (int i = 0; i < piles; i++) {
				pile[i] = new ArrayList<>();

				int cards = (i < 4) ? 5 : 4;

				for (int j = 0; j < cards; j++) {
					Cards card = draw();

					card.width = cardWidth;
					card.height = cardHeight;

					pile[i].add(card);
				}

				fixPile(i);
			}
		}

		private Cards draw() {
			Cards card = allCards.get(0);
			allCards.remove(card);
			return card;
		}

		public void loadImages(String difficulty) {
			this.difficulty = difficulty;

			int value = -1;

			if (difficulty.equals("Easy")) {
				value = 4;
			} else if (difficulty.equals("Medium")) {
				value = 3;
			} else {
				value = 1;
			}

			allCards = new ArrayList<>();

			int counter = 0;

			while (counter < 8) {
				for (int suit = value; suit <= 4; suit++) {
					for (int rank = 1; rank <= 13; rank++) {
						allCards.add(new Cards(readImage("/res/" + rank + "" + suit + ".png"), suit, rank, true));
					}

					counter++;
				}
			}
		}

		private void showPlayAgainNoWin() {
			int resp = JOptionPane.showConfirmDialog(
					this, "NON CI SONO PIU' MOSSE POSSIBILI \n" + "PUNTI TOTALIZZATI: " + board.score
							+ "\n" + "MOSSE EFFETTUATE: " + board.moves + "\n" + "Do you want to play again?",
					"Game over!", JOptionPane.YES_NO_OPTION);

			if (resp == JOptionPane.YES_OPTION) {
				newGame();
			} else {
				System.exit(0);
			}
		}

		private void showPlayAgainDialog() {
			int resp = JOptionPane.showConfirmDialog(this, "Points: " + score + "\n" + "Do you want to play again?",
					"Game over!", JOptionPane.YES_NO_OPTION);

			if (resp == JOptionPane.YES_OPTION) {
				newGame();
			} else {
				System.exit(0);
			}
		}

		private boolean checkForCardsToRemove(int index) {
			int suit = -1;
			int rank = 1;

			for (int i = pile[index].size() - 1; i >= 0 && rank <= 13; i--, rank++) {
				Cards card = pile[index].get(i);

				if (suit == -1) {
					suit = card.getSuit();
				}
				if (suit != card.getSuit()) {
					return false;
				}
				if (card.isFaceDown()) {
					return false;
				}
				if (card.getRank() != rank) {
					return false;
				}
			}

			if (rank == 14) {
				
				try {
					Thread.sleep(1250);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				
				int y = height - cardHeight - margin - 40 - insets.bottom - 16;

				Cards prevCard = (ptr == -1) ? null : allCards.get(ptr);

				for (int i = pile[index].size() - 1; i >= 0 && --rank >= 1; i--) {
					Cards card = pile[index].get(i);

					card.x = (prevCard == null) ? margin : prevCard.x + margin + 2;
					card.y = y;
					card.flip(); // the card must be flipped face down for new game

					pile[index].remove(card);
					allCards.add(card);
					ptr++;
				}

				Cards last = (pile[index].size() > 0) ? pile[index].get(pile[index].size() - 1) : null;

				if (last != null && last.isFaceDown()) {
					last.flip();
				}
				cont++;
				return true;
			}

			return false;
		}
	}

	class GameState {
		List<Cards> allCards;

		List<Cards>[] pile;
		List<Cards>[] deck;

		int top;
		int ptr;

		boolean dealed;

		public GameState() {
			dealed = true;
		}

		@SuppressWarnings("unchecked")
		public GameState(List<Cards> allCards, List<Cards>[] pile, List<Cards>[] deck, int top, int ptr) {
			this.allCards = new ArrayList<>();

			for (Cards card : allCards) {
				this.allCards.add((Cards) card.clone());
			}

			this.pile = new List[board.piles];

			for (int i = 0; i < board.piles; i++) {
				this.pile[i] = new ArrayList<>();

				for (Cards card : pile[i]) {
					this.pile[i].add((Cards) card.clone());
				}
			}

			this.deck = new List[board.slots];

			for (int i = 0; i < board.slots; i++) {
				if (deck[i] == null) {
					this.deck[i] = null;
				} else {
					this.deck[i] = new ArrayList<>();

					for (Cards card : deck[i]) {
						this.deck[i].add((Cards) card.clone());
					}
				}
			}

			this.top = top;
			this.ptr = ptr;

			dealed = false;
		}

		public List<Cards> getAllCards() {
			return allCards;
		}

		public List<Cards>[] getPile() {
			return pile;
		}

		public List<Cards>[] getDeck() {
			return deck;
		}

		public int getTop() {
			return top;
		}

		public int getPtr() {
			return ptr;
		}

		public boolean dealed() {
			return dealed;
		}
	}

	private class Cards extends Rectangle {
		private Image cardImage;

		private int suit;
		private int rank;

		private boolean isFaceDown;

		public Cards(Image cardImage, int suit, int rank, boolean isFaceDown) {
			super();
			this.cardImage = cardImage;
			this.suit = suit;
			this.rank = rank;
			this.isFaceDown = isFaceDown;
		}

		public void flip() {
			isFaceDown = !isFaceDown;
		}

		public Image getImage() {
			return cardImage;
		}

		public int getSuit() {
			return suit;
		}

		public int getRank() {
			return rank;
		}

		public boolean isFaceDown() {
			return isFaceDown;
		}

		@Override
		public Object clone() {
			Cards copy = new Cards(cardImage, suit, rank, isFaceDown);

			copy.x = x;
			copy.y = y;
			copy.width = width;
			copy.height = height;

			return copy;
		}

		@Override
		public String toString() {
			return rank + "," + suit;
		}
	}
}