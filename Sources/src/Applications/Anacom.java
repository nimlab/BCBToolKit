package Applications;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import Config.BCBEnum;
import Config.BCBEnum.Param;
import IHM.BCBToolKitIHM;
import IHM.Browser;
import IHM.ImagePanel;
import IHM.LoadingBar;
import IHM.Tools;
import Models.AnacomModel;

public class Anacom extends AbstractApp {
	public static final String ANACOM_TITLE = "AnaCOM2";
	public static final int FRAME_WIDTH = 330;
	public static final int FRAME_HEIGHT = 580;

	private ImagePanel background;
	private JPanel panel;
	private JPanel topP;
	private JButton settings;
	private JButton run;
	private LoadingBar loading;
	
	private JComboBox<String> modeCombo;

	private JComboBox<String> testCombo;
	private Browser lesBro;
	private Browser resBro;
	private Browser csvBro;
	private Browser ctrlBro;
	private JTextField threshFld;
	private JCheckBox saveTmp;
	private JTextField jftf;

	//Radio buttons to select the control value mode
	private JRadioButton meanMode;
	private JRadioButton vectMode;
	private ButtonGroup bg;

	private AnacomModel model;
	

	public Anacom(String path, BCBToolKitIHM b) {
		super(path, b, BCBEnum.Index.ANACOM);
	}

	protected void createModel() {
		model = new AnacomModel(this.getPath(), this.getFrame());
	}

	protected void createView() {
		int osxisthedevil = 0;
		if (Tools.isOSX()) {
			osxisthedevil = 30;
		}
		//Frame
		frame = new JFrame(ANACOM_TITLE); {
			frame.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT + osxisthedevil));
			frame.setResizable(false);
			display();
			frame.setFocusable(true);
		}
		background = new ImagePanel("anacom.png", 135, 110);
		background.setPreferredSize(new Dimension(FRAME_WIDTH, LINE_HEIGHT * 6));
		//Création des icones
		URL url = getClass().getClassLoader().getResource("settings.png");
		Icon setIco = new ImageIcon(url); 
		int setIcoW = setIco.getIconWidth();
		//Création des boutons
		int padding = 0;
		if (!Tools.isOSX()) {
			padding = -5;
		}
		settings = new JButton(setIco);
		settings.setPreferredSize(new Dimension(setIcoW + ICON_PADDING, setIcoW + ICON_PADDING + padding));
		settings.setToolTipText("Settings");

		run = new JButton("RUN");
		run.setPreferredSize(new Dimension(FRAME_WIDTH - 10, 45));


		String[] tab = {"Kruskal-Wallis", "Mann-Whitney", "t-test", "Kolmogorov-Smirnov"};
		testCombo = new JComboBox<String>(tab);
		String test_conf_val = conf.getVal(Param.ADEFTEST);
		if (!test_conf_val.equals("")) {
			testCombo.setSelectedIndex(Integer.valueOf(test_conf_val));
		}
		String[] tab_mode = {"No post-hoc test", "Disconnected versus spared", "Disconnected versus controls", 
				"Spared versus controls"};
		modeCombo = new JComboBox<String>(tab_mode);
		String mode_conf_val = conf.getVal(Param.ADEFMODE);
		if (!mode_conf_val.equals("")) {
			modeCombo.setSelectedIndex(Integer.valueOf(mode_conf_val));
		}
		modeCombo.setEnabled(((String)testCombo.getSelectedItem()).equals("Kruskal-Wallis"));
		lesBro = new Browser(this.getFrame(), "Lesions directory :", BCBEnum.fType.DIR.a(),
				this.getConf(), BCBEnum.Param.ALESDIR, this.getBCB());
		resBro = new Browser(this.getFrame(), "Result directory :", BCBEnum.fType.DIR.a(),
				this.getConf(), BCBEnum.Param.ARESDIR, this.getBCB());
		csvBro = new Browser(this.getFrame(), "Patient names & scores (.csv file) :", BCBEnum.fType.CSV.a(),
				this.getConf(), BCBEnum.Param.ACSVFILE, this.getBCB());
		ctrlBro = new Browser(this.getFrame(), "", BCBEnum.fType.CSV.a(),
				this.getConf(), BCBEnum.Param.ACTRLFILE, this.getBCB());
		getBCB().addBro(lesBro.getParam(), lesBro);
		getBCB().addBro(csvBro.getParam(), csvBro);
		getBCB().addBro(resBro.getParam(), resBro);
		getBCB().addBro(ctrlBro.getParam(), ctrlBro);


		saveTmp = new JCheckBox("Keep temporary files");
		saveTmp.setPreferredSize(new Dimension(260, 20));
		saveTmp.setIconTextGap(20);
		saveTmp.setSelected(conf.getVal(BCBEnum.Param.ASAVETMP).equals("true"));
		String thresh = conf.getVal(BCBEnum.Param.ATHRESH);
		if (thresh.equals("")) {
			thresh = "3";
		}
		threshFld = new JTextField(thresh);
		threshFld.setHorizontalAlignment(JTextField.RIGHT);
		threshFld.setPreferredSize(new Dimension(40, LINE_HEIGHT));

		jftf = new JTextField();
		jftf.setHorizontalAlignment(JTextField.RIGHT);
		jftf.setPreferredSize(new Dimension(80, LINE_HEIGHT));

		meanMode = new JRadioButton("Or published normative value :", false);
		meanMode.setPreferredSize(new Dimension(FRAME_WIDTH - 10, LINE_HEIGHT));
		vectMode = new JRadioButton("Control scores (.csv file) :", true);
		vectMode.setPreferredSize(new Dimension(FRAME_WIDTH - 10, LINE_HEIGHT));


		if (conf.getVal(BCBEnum.Param.ACTRLFILE).equals("") &&
				!conf.getVal(BCBEnum.Param.ACTRLMEAN).equals("")) {
			meanMode.setSelected(true);
			jftf.setText(conf.getVal(BCBEnum.Param.ACTRLMEAN));
		}

		if (meanMode.isSelected()) {
			jftf.setEnabled(true);
			vectMode.setSelected(false);
			ctrlBro.deactivate();
		}
		if (vectMode.isSelected()) {
			ctrlBro.activate();
			meanMode.setSelected(false);
			jftf.setEnabled(false);
		}

		bg = new ButtonGroup(); {
			bg.add(vectMode);
			bg.add(meanMode);
		}
	}

	protected void placeComponents() {

		JPanel testSelector = new JPanel(new GridLayout(2,0)); {

			JPanel c_down = new JPanel(new FlowLayout(FlowLayout.CENTER));
			c_down.add(modeCombo);
			JPanel c_top = new JPanel(new FlowLayout(FlowLayout.CENTER));
			c_top.add(testCombo);
			testSelector.add(c_top);
			testSelector.add(c_down);
		}

		JPanel center = new JPanel(new GridLayout(3, 0)); {
			center.add(lesBro);
			center.add(resBro);
			center.add(csvBro);
		}

		topP = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topP.setPreferredSize(new Dimension(FRAME_WIDTH, LINE_HEIGHT));
		frame.add(topP);
		frame.add(background, BorderLayout.NORTH);

		JPanel thr = new JPanel(new FlowLayout(FlowLayout.CENTER)); {
			JLabel l = new JLabel("Overlap threshold : ");
			thr.add(l);
			thr.add(threshFld);
		}
		JPanel centerGrid = new JPanel(new BorderLayout()); {
			//centerGrid.setLayout(new BoxLayout(centerGrid, BoxLayout.Y_AXIS));
			// A box layout could be more logical but ... it works
			JPanel middle_top = new JPanel(new BorderLayout()); {
				middle_top.add(testSelector, BorderLayout.NORTH);
				middle_top.add(center, BorderLayout.CENTER);
				middle_top.add(thr, BorderLayout.SOUTH);
			}

			JPanel bg1 = new JPanel(new GridLayout(4, 0)); {
				bg1.add(vectMode);
				bg1.add(ctrlBro);
				bg1.add(meanMode);
				JPanel fl = new JPanel(new FlowLayout(FlowLayout.RIGHT)); {
					fl.add(jftf);
				}
				bg1.add(fl);
			}
			centerGrid.add(middle_top, BorderLayout.NORTH);
			centerGrid.add(bg1, BorderLayout.CENTER);
		}
		frame.add(centerGrid, BorderLayout.CENTER);

		// Panel contenant les deux checkbox
		JPanel r1 = new JPanel(new FlowLayout(FlowLayout.RIGHT)); {
			r1.add(saveTmp);
			r1.add(settings);
		}
		// Panel du bouton run
		panel = new JPanel(new FlowLayout(FlowLayout.CENTER)); {
			panel.add(run);
			panel.setPreferredSize(new Dimension(FRAME_WIDTH - 10, 55));
		}
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT)); {
			south.add(r1);
			south.add(panel);
		}
		int vgap = 0;
		if (Tools.isOSX()) {
			vgap = 10;
		}
		south.setPreferredSize(new Dimension(FRAME_WIDTH, 55 + 45 + vgap));		
		frame.add(south, BorderLayout.SOUTH);
	}

	protected void createControllers() {
		Tools.gatherRound(this);
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				closing();
			}
		});

		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				worker = new SwingWorker<Void, Void>() {
					@Override
					public Void doInBackground() {
						boolean containsZero = false;
						// We have to check if resBro is correctly filled because
						// we use its path after
						if (Tools.isReady(frame, resBro)) {
							model.setResultDir(resBro.getPath());
						} else {
							return null;
						}
						String testName = (String)testCombo.getSelectedItem();
						model.setTest(testName);
						
						String ph_name = (String)modeCombo.getSelectedItem();
						model.setMode(ph_name);
						// We test if the string is an integer
						try {
							Integer.parseInt(threshFld.getText());
						} catch (NumberFormatException e) {
							Tools.showErrorMessage(frame, "The threshold must be an integer");
							return null;
						}
						model.setThreshold(threshFld.getText());

						if (Tools.isReady(frame, csvBro)) {
							String copypath = resBro.getPath() + "/copypatients.csv";
							containsZero = Tools.cleanCopy(csvBro.getPath(), copypath, true, frame) 
									|| containsZero ;
							if (containsZero) {
								Tools.showErrorMessage(frame, csvBro.getPath() + " contains " + 
										"values <= 0, please correct those " + 
										"values before launching AnaCOM2");
								return null;
							}
							model.setCSV(copypath);
						} else {
							return null;
						}
						
						if (meanMode.isSelected()) {
							if (parseMeanField(jftf.getText()).equals("")) {
								Tools.showErrorMessage(frame, "The mean must be a number" +
										" (Integer or Decimal)");
								return null;
							}
							if (testName.equals("Kolmogorov-Smirnov")) {
								Tools.showErrorMessage(frame, "You cannot execute the " +
										"Kolmogorov-Smirnov test with only a \n mean for controls");
								return null;
							}
							containsZero = (Float.valueOf(jftf.getText()) != 0) || containsZero;
							model.setControls(jftf.getText());
						} else {
							if (Tools.isReady(frame, ctrlBro)) {
								String copypath = resBro.getPath() + "/copycontrols.csv";
								containsZero = Tools.cleanCopy(ctrlBro.getPath(), copypath, true, frame)
										|| containsZero;
								if (containsZero) {
									Tools.showErrorMessage(frame, csvBro.getPath() + " contains " + 
											"values <= 0, please correct those " + 
											"values before launching AnaCOM2");
									return null;
								}
								model.setControls(copypath);
							} else {
								return null;
							}
						}
						// Should we save temporary files
						if (saveTmp.isSelected()) {
							model.setSaveTmp("true");
						} else {
							model.setSaveTmp("false");
						}

						if (Tools.isReady(frame, lesBro)) {
							model.setLesionDir(lesBro.getPath());
						} else {
							return null;
						}
						model.setDetZero("false");
						model.setNbVox(getBCB().getSettings().getNbVox());
						model.run();
						return null;
					}

					@Override
					protected void done() {
						changeRunButton(panel, 1);
					}
				};
				changeRunButton(panel, 0);
				worker.execute();
			}
		});
		
		threshFld.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				String text = threshFld.getText();
				//Deleting of invisible characters
				text = text.trim();
				try {
					Integer.parseInt(text);
				} catch (NumberFormatException n) {
					Tools.showErrorMessage(getBCB().getFrame(), 
							"The minimum of overlaps must be an integer >= 3");
					threshFld.setText("3");
					return;
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		
		testCombo.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!testCombo.getSelectedItem().equals("Kruskal-Wallis")) {
					modeCombo.setEnabled(false);
				} else {
					modeCombo.setEnabled(true);
				}
				
			}
		});

		vectMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vectMode.isSelected()) {
					ctrlBro.activate();
					meanMode.setSelected(false);
					jftf.setEnabled(false);
				}
			}
		});
		meanMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (meanMode.isSelected()) {
					jftf.setEnabled(true);
					vectMode.setSelected(false);
					ctrlBro.deactivate();
				}
			}
		});
		
		saveTmp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (saveTmp.isSelected()) {
					conf.setVal(BCBEnum.Param.ASAVETMP, "true");
				} else {
					conf.setVal(BCBEnum.Param.ASAVETMP, "false");
				}
			}
		});
		
		settings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getBCB().openSettings(BCBEnum.Index.ANACOM);
			}
		});
	}
	
	private String parseMeanField(String str) {
		if (str.matches("[0-9]*.[0-9]*|[0-9]*")) {
			return str;
		} else {
			return "";
		}
	}

	protected void changeRunButton(JPanel p, int state) {
		// 0 = lancement de run() 1 = Fin de run()
		if (state == 0) {
			loading = new LoadingBar();
			p.remove(run);
			loading.setToolTipText("Loading");
			p.add(loading);
			model.setLoadingBar(loading);
			loading.setWidth(0);
			p.repaint();
			p.revalidate();
		} else if (state == 1) {
			p.remove(loading);
			p.add(run);
			p.repaint();
			p.revalidate();
		} else {
			return;
		}
	}
	
	public void closing() {
		if (vectMode.isSelected()) {
			conf.setVal(Param.ACTRLMEAN, "");
		} else {
			conf.setVal(Param.ACTRLMEAN, parseMeanField(jftf.getText()));
			//We reset the path of ctrlBro
			ctrlBro.setPath("", null);
		}
		conf.setVal(Param.ADEFTEST, String.valueOf(testCombo.getSelectedIndex()));
		conf.setVal(Param.ADEFMODE, String.valueOf(modeCombo.getSelectedIndex()));
		conf.setVal(BCBEnum.Param.ATHRESH, threshFld.getText());
		super.closing();
	}
	
	@Override
	public void cancel() {
		if (worker != null) {
			Tools.cancelActions("", worker);
		}
		model.stopProcess();
	}

	@Override
	public void stopProcess() {
		model.stopProcess();
	}
}
