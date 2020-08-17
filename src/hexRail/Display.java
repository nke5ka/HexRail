package hexRail;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Display extends JPanel {
	private WorldOfChooChoo infoService;
	
	MapDisplay viewscr;
	Timer tickTimer = new Timer(16, new TickListener());
	
	private static final long serialVersionUID = 1L;
	public Display(WorldOfChooChoo dataHolder) {
		infoService = dataHolder;
		// Initialize left screen holding the output images, and a UI screen for inputs
		setLayout(new BorderLayout());
		viewscr	= new MapDisplay(dataHolder);
		JTabbedPane sideBar	= new JTabbedPane();
		
		sideBar.add("Terrain", new TerrainControls());
		sideBar.add("City",    new CityControls());
		sideBar.add("Routes",  new RouteControls());
		sideBar.add("Trains", new TrainControls());
		
		viewscr.addComponentListener(new ResizeListener());
		
		add(viewscr, BorderLayout.CENTER);
		add(sideBar, BorderLayout.EAST);
		
		//tickTimer.start();
	}
	
	private class TerrainControls extends JPanel {
		private static final long serialVersionUID = 1L;
		//UI THINGS
		JSpinner[]	terrainBounds = new JSpinner[5];
		JSpinner	hexRowsSpin, hexColsSpin, perturbsSpin, smoothsSpin;
		
		public TerrainControls() {
		//User controlled settings for terrain generation
			setLayout(new GridLayout(13,2));
			
			hexRowsSpin  = spinnerCreator("Number of Hexes High",	infoService.getHexRows(),	this);
			hexColsSpin  = spinnerCreator("Number of Hexes Wide",	infoService.getHexCols(),	this);
			perturbsSpin = spinnerCreator("Perturbation",			infoService.getPerturbs(), 	this);
			smoothsSpin  = spinnerCreator("Smoothings",				infoService.getSmoothings(),this);
			
			// Add empty spaces
			add(new JLabel());
			add(new JLabel());
			
			String[] terrainNames = {"OceanDeepestCutoff", "SandMin", "GrassMin", "Mountain", "Snow Peak"};
			for (int terrainLevel = 0; terrainLevel < 5; terrainLevel++) {
				terrainBounds[terrainLevel] = spinnerCreator(terrainNames[terrainLevel], infoService.getRange()[terrainLevel+1], this);
			}
			
			//check boxes for grid line and discrete boundaries
			Checkbox showGridChk = new Checkbox("Show Grid");
			Checkbox discreteChk = new Checkbox("No Intermediate Terrain Colors");
			ItemListener settingsNotify = new SettingsChkListener(showGridChk, discreteChk);
			showGridChk.addItemListener(settingsNotify);
			discreteChk.addItemListener(settingsNotify);
			add(showGridChk);
			add(discreteChk);
			//The buttons to print and generate
			buttonCreator("SavePNG", new PNGOutListener(), this);
			buttonCreator("Generate New Terrain", new GenerationBeginListener(), this);
			
			infoService.landGen();
		}
		
		private class GenerationBeginListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				// When the Generate New Terrain button is pressed, get settings
				// force height and width to be even
				hexRowsSpin.setValue((Integer)hexRowsSpin.getValue()-((Integer)hexRowsSpin.getValue())%2);
				hexColsSpin.setValue((Integer)hexColsSpin.getValue()-((Integer)hexColsSpin.getValue())%2);
				infoService.setHexRows((Integer)hexRowsSpin.getValue());
				infoService.setHexCols((Integer)hexColsSpin.getValue());
				infoService.setPerturbs((Integer)perturbsSpin.getValue());
				infoService.setSmoothings((Integer)smoothsSpin.getValue());
				for (int i = 0; i < 5; i++) {
					infoService.getRange()[i+1] = (Integer)terrainBounds[i].getValue();
				}
				// Send desired settings to MapDisplay to generate
				infoService.landGen();
				viewscr.draw();
				viewscr.repaint();
			}
		}
		
		private class SettingsChkListener implements ItemListener {
			private Checkbox gridC, discrC;
			public SettingsChkListener(Checkbox gr, Checkbox discr) {
				gridC  = gr;
				discrC = discr;
			}
			public void itemStateChanged(ItemEvent e) {
				viewscr.setChkSettings(gridC.getState(), discrC.getState());
				viewscr.repaint();
			}
		}
	}

	private class CityControls extends JPanel {
		private static final long serialVersionUID = 1L;
		//CITY STUFF
		JSpinner	numCitiesSpin, cityLowerSpin, cityUpperSpin;
		public CityControls() {
			//User controls for generating cities
			setLayout(new GridLayout(4, 2)); //(10,2) or whatever later
			numCitiesSpin = spinnerCreator("Number of Cities", 5, this);
			cityLowerSpin = spinnerCreator("City Lower Bound", infoService.getCityLower(), this);
			cityUpperSpin = spinnerCreator("City Upper Bound", infoService.getCityUpper(), this);
			buttonCreator("Remove All Cities",	new RemoveCitiesListener(), this);
			buttonCreator("Found Cities",		new FoundCitiesListener(),  this);
		}
		
		private class RemoveCitiesListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				infoService.removeCities();
				viewscr.draw();
				viewscr.repaint();
			}
		}
		
		private class FoundCitiesListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				//Get settings for city generation;
				infoService.setCityLower((Integer)cityLowerSpin.getValue());
				infoService.setCityUpper((Integer)cityUpperSpin.getValue());
				infoService.foundCities((Integer)numCitiesSpin.getValue());
				viewscr.draw();
				viewscr.repaint();
			}
		}
	}

	private class RouteControls extends JPanel {
		private static final long serialVersionUID = 1L;
		public RouteControls() {
			//User controls for generating routes between two cities
			setLayout(new GridLayout(5, 2));
			add(new JLabel("City A"));
			add(new JLabel("SELECT PLACEHOLDER"));
			add(new JLabel("City B"));
			add(new JLabel("SELECT PLACEHOLDER"));
			buttonCreator("Connect 2 Cities[TODO]",				new Connect2CitiesListener(),	this);
			buttonCreator("Connect 2 Random Cities",			new ConnectRandCitiesListener(),this);
			buttonCreator("Clear All Paths",					new ClearPathsListener(),		this);
			buttonCreator("Connect All Cities",					new ConnectAllCitiesListener(),	this);
			buttonCreator("Prune to Minimal Spanning Tree(s)",	new PruneListener(),			this);
		}
		
		private class ConnectRandCitiesListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				infoService.connectRandCities();
				viewscr.draw();
				viewscr.repaint();
			}
		}
		
		private class Connect2CitiesListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				//viewscr.connect2Cities();;
				infoService.connectRandCities();
				viewscr.draw();
				viewscr.repaint();
			}
		}
		
		private class ClearPathsListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				infoService.clearAllPaths();
				viewscr.draw();
				viewscr.repaint();
			}
		}
		
		private class ConnectAllCitiesListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				infoService.connectAllCities();
				viewscr.draw();
				viewscr.repaint();
			}
		}
		
		private class PruneListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				infoService.prunePaths();
				viewscr.draw();
				viewscr.repaint();
			}
		}
	}

	private class TrainControls extends JPanel {
		private static final long serialVersionUID = 1L;
		//TRAIN STUFF
		private JLabel speedIndicator;
		private JSlider speedSlider;
		private JButton newTrainButton, leftButton, rightButton, renameButton, onOffButton, delButton;
		public TrainControls() {
			//User controls for the trains
			setLayout(new GridLayout(5, 1));
			
			JPanel speedPanel = new JPanel(new GridLayout(1, 2));
			speedIndicator = new JLabel("SPEED: PAUSED");
			speedPanel.add(speedIndicator);
			speedSlider = new JSlider(0, 5, 0);
			Hashtable<Integer, JLabel> speedopt = new Hashtable<Integer, JLabel>(10);
			speedopt.put(new Integer(0), new JLabel("x0"));
			speedopt.put(new Integer(1), new JLabel("x1/2"));
			speedopt.put(new Integer(2), new JLabel("x1"));
			speedopt.put(new Integer(3), new JLabel("x2"));
			speedopt.put(new Integer(4), new JLabel("x4"));
			speedopt.put(new Integer(5), new JLabel("x8"));
			speedSlider.setLabelTable(speedopt);
			speedSlider.setSnapToTicks(true);
			speedSlider.setPaintLabels(true);
			speedSlider.addChangeListener(new SpeedListener());
			speedPanel.add(speedSlider);
			add(speedPanel, BorderLayout.NORTH);
			
			
			
			//Show controls for a train
			JPanel trainControl = new JPanel(new GridLayout(2, 3));
			newTrainButton = new JButton("NEW TRAIN");
			newTrainButton.addActionListener(new TrainButtonsListener(0));
			trainControl.add(newTrainButton);
			leftButton = new JButton("<");
			leftButton.setEnabled(false);
			leftButton.addActionListener(new TrainButtonsListener(1));
			trainControl.add(leftButton);
			rightButton = new JButton(">");
			rightButton.addActionListener(new TrainButtonsListener(2));
			trainControl.add(rightButton);
			renameButton = new JButton("RENAME");
			renameButton.addActionListener(new TrainButtonsListener(3));
			trainControl.add(renameButton);
			onOffButton = new JButton("[GO]/STOP");
			onOffButton.addActionListener(new TrainButtonsListener(4));
			trainControl.add(onOffButton);
			delButton = new JButton("DELETE");
			delButton.addActionListener(new TrainButtonsListener(5));
			trainControl.add(delButton);
			add(trainControl, BorderLayout.NORTH);
		}
		
		private class TrainButtonsListener implements ActionListener {
			private int type;
			public TrainButtonsListener(int t) {
				type = t;
			}
			public void actionPerformed(ActionEvent e) {
				switch(type) {
					case 0: newTrain(); return;
					case 1: left(); return;
					case 2: right(); return;
					case 3: rename(); return;
					case 4: switchR(); return;
					case 5: delete(); return;
				}
			}
			/*public void setButtonsEnabled() {
				if (infoService.getTrains().size() == 0) {
					setBorder(BorderFactory.createTitledBorder("NO TRAINS EXIST"));
					renameButton.setEnabled(false);
					onOffButton.setEnabled(false);
					onOffButton.setText("GO/STOP");
					delButton.setEnabled(false);
				} else {
					renameButton.setEnabled(true);
					onOffButton.setEnabled(true);
					delButton.setEnabled(true);
				}
				if (infoService.getTrains().size() < 2) {
					leftButton.setEnabled(false);
					rightButton.setEnabled(false);
				} else {
					leftButton.setEnabled(true);
					rightButton.setEnabled(true);
				}
			}*/
		}
		
		private class SpeedListener implements ChangeListener {
			public void stateChanged(ChangeEvent e) {
				switch (speedSlider.getValue()) {
					case 0: speedIndicator.setText("SPEED: PAUSED");tickTimer.stop();  break;
					case 1: speedIndicator.setText("SPEED: X 1/2");	tickTimer.start(); tickTimer.setDelay(256); break;
					case 2: speedIndicator.setText("SPEED: X 1");	tickTimer.start(); tickTimer.setDelay(128); break;
					case 3: speedIndicator.setText("SPEED: X 2");	tickTimer.start(); tickTimer.setDelay( 64);  break;
					case 4: speedIndicator.setText("SPEED: X 4");	tickTimer.start(); tickTimer.setDelay( 32);  break;
					case 5: speedIndicator.setText("SPEED: X 8");	tickTimer.start(); tickTimer.setDelay( 16);  break;
				}
			}
		}
		
		private void newTrain() {
			
		}
		
		private void left() {
			
		}
		
		private void right() {
			
		}
		
		private void rename() {
			
		}
		
		private void switchR() {
			
		}
		
		private void delete() {
			
		}
	}
	
	private class TickListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			viewscr.draw();
			viewscr.repaint();
		}
	}
	
	private JSpinner spinnerCreator(String labelName, int initialValue, JPanel addToPanel) {
		addToPanel.add(new JLabel(labelName));
		JSpinner spinnerToAdd =	new JSpinner();
		spinnerToAdd.setValue(initialValue);
		addToPanel.add(spinnerToAdd);
		return spinnerToAdd;
	}
	
	private void buttonCreator(String buttonName, ActionListener actionPerformer, JPanel addToPanel) {
		JButton buttonToAdd = new JButton(buttonName);
		buttonToAdd.addActionListener(actionPerformer);
		addToPanel.add(buttonToAdd);
	}
	
	class ResizeListener extends ComponentAdapter {
		public void componentResized(ComponentEvent e) {
			//When screen is adjusted, let the drawing system know
			//System.out.println(output.getWidth() + " " + output.getHeight());
			viewscr.resizeCalculations(viewscr.getWidth(), viewscr.getHeight());
			viewscr.draw();
			viewscr.repaint();
		}
	}
	
	private class PNGOutListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			viewscr.outputPNG();
		}
	}
}