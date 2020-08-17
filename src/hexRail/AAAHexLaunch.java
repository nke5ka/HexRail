package hexRail;

import javax.swing.JFrame;

public class AAAHexLaunch {
	//Launches HexRail!
	public static void main(String[] args) throws Exception {
		// Todo: maybe use singletons or some other pattern for handling global state
		WorldOfChooChoo infoService = new WorldOfChooChoo();
		JFrame frame = new JFrame("City Networks Generation");
		frame.setSize(1300, 720);
		frame.setLocation(50, 50);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new Display(infoService));
		frame.setVisible(true);
	}
}