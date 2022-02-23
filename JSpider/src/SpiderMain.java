package src;

import java.io.IOException;

import it.unical.mat.embasp.languages.IllegalAnnotationException;
import it.unical.mat.embasp.languages.ObjectNotValidException;

public class SpiderMain {
	
	public static void main(String[] args) throws IOException, ObjectNotValidException, IllegalAnnotationException {
	
		JSpider game = new JSpider();
//		game.setVisible(true);
		game.run();
	
	}
	
}
