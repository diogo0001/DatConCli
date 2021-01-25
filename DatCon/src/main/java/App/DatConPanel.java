/**
DatCon class

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that redistribution of source code include
the following disclaimer in the documentation and/or other materials provided
with the distribution.

THIS SOFTWARE IS PROVIDED BY ITS CREATOR "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE CREATOR OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-i "C:\dev\DatCon-3.6.3\DatCon Files\Operation_Cheez_Whiz" 
-w -=
*/

package App;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import Files.AnalyzeDatResults;
import Files.ConvertDat;
import Files.DJIAssistantFile;
import Files.DatConLog;
import Files.DatConPopups;
import Files.DatFile;
import Files.FileBeingUsed;
import Files.Persist;
import Files.WorkingDir;
import GUI.CheckUpdates;
import GUI.CsvPanel;
import GUI.DatConMenuBar;
import GUI.KMLPanel;
import GUI.LogFilesPanel;
import GUI.LoggingPanel;
import GUI.TimeAxisPanel;

public class DatConPanel extends JPanel
		implements ActionListener, ComponentListener, MouseListener {

	// Constants
	static private final long serialVersionUID = 1;

	// Globals
    private int windowCnt = 0;
    private boolean isCommandLine = false;
    private boolean wantUI = false;

	// Instance variables
	public CheckUpdates checkUpdates = null;
	private String datFileName = "";
	public String outputDirPath = "";
	public File inputFile = null;
	public File outputDir = null;
	public DatFile datFile = null;
	private boolean exception = false;
	private boolean isWorking = true;

	//   UI components:
	private JFrame frame = null;
	private Component glassPane = null;
	public DatConMenuBar menuBar = null;
	private JPanel contentPanel = null;
//  private Color contentPaneBGColor = null;
	private JTextField datFileTextField = new JTextField(
	         "Click here to specify the input .DAT file");
	private JTextField outputDirTextField = new JTextField(
	         "Click here to specify the output directory");
	private JButton dirViewIt = new JButton("View It");
	public TimeAxisPanel timeAxisPanel = null;
	private CsvPanel csvPanel;
	private LogFilesPanel logFilesPanel = null;
	public KMLPanel kmlPanel = null;
	public JButton goButton = new JButton("Go!");
	public LoggingPanel logPanel = null;
	private Timer resizeTimer = null;
	private JFileChooser fc = null;
	private JFileChooser dc = null;

	protected DatConPanel(File inFile, File outFile,
			boolean _isCommandLine, boolean _wantUI) {
		isCommandLine = _isCommandLine;
		wantUI = _wantUI;

	 	checkUpdates = new CheckUpdates(this);

	 	if (!isCommandLine) {
	        createGUI(0);
	        goButton.setEnabled(true);
	    } else {
	        // Set up the .DAT file processing log
	        DatConLog log = new DatConLog(inFile, outFile);
	        if (!log.ok()) {
	            if (inFile == null) { // Got GUI
	            	DatConPopups.noLogFile();
	            } else {
	            	System.out.println("No log file!");
	            }
	        }

			// createGUI(windowCnt++);
	        goButton.setEnabled(false);

	        goButton.setForeground(Color.BLACK);
	        goButton.setText("Checking for .DAT compatibility...");

	 		try {
	 	        // Set up the output directory, which may fail
	 	        outputDirPath = (outFile != null ? outFile.getAbsolutePath()
	 	                                         : Persist.outputDirName);
	 			File outDirFile = new File(outputDirPath);
	 			if (!outDirFile.exists()) {
	 				outFile.createNewFile();
	 			}
	 			setOutputDir(outDirFile);
	 	    	if (inFile != null) {
	 	        	setInputFile(inFile);
	 	    	}
	 	        checkIOState();

	 	        if (inputFile.exists() /*&& Persist.loadLastOnStartup*/) {
	 	        	/*
	 	        	 * Begin DatCon processing:  Pre-Analyze, then Analyze.
	 	        	 */
	 	            setDatFile(inputFile);
	 	        } else {
	 	            File inputDir = inputFile.getParentFile();
	 	            fc.setCurrentDirectory(inputDir);
	 	        }
	 	    }
	 		catch (IOException e) {
	             DatConLog.Exception(e);
	             setIsWorking(false);
	 		}
	 	}
	}

	public JFrame getFrame() {
	 	return frame;
	}
 
	synchronized
	private boolean isWorking() {
	 	return isWorking;
	}
	synchronized
	private void setIsWorking(boolean working) {
	 	isWorking = working;
	}

	public boolean isException() {
	 	return exception;
	}

	public void showException(Exception e, String msg) {
	 	exception = true;
	 	if (msg == null) {
	 		msg = e.getMessage();
	 	}
	 	logPanel.Exception(e, msg);
	}

	public void showInfo(String msg) {
	 	logPanel.Info(msg);
	}

	private void createGUI(int idx) {
	 	exception = false; // Reset

	    try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
	        UIManager.put("ToolTip.background", Color.WHITE);
	        UIManager.put("ToolTip.foreground", Color.BLACK);

	        FileNameExtensionFilter filter = new FileNameExtensionFilter(
	                "DAT file", "DAT");
	        fc = new JFileChooser(/* directory */);
	        //Action folder = fc.getActionMap().get("New Folder");
	        //folder.setEnabled(false);
	        fc.setAcceptAllFileFilterUsed(false);
	        fc.addChoosableFileFilter(filter);
	        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

	        dc = new JFileChooser();
	        dc.setAcceptAllFileFilterUsed(false);
	        dc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	     	frame = new JFrame("DatCon");
	        //frame.addComponentListener(new ComponentAdapter() {
	        //    public void componentResized(ComponentEvent evt) {
	        //        Component c = (Component) evt.getSource();
	        //        int x = 1;
	        //    }
	        //});
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        menuBar = new DatConMenuBar(this);
	        frame.setJMenuBar(menuBar);

	        // Create and set up the content pane
	        Container content = createContentPane();
	        frame.setContentPane(content);
	        frame.setSize(Persist.datConSize);
	        frame.setIconImage(new ImageIcon("drone.jpg").getImage());
	        if (isCommandLine) {
	         	String title = frame.getTitle();
	         	if (idx > 0) {
	        		title += -idx;
	        	}
	        	frame.setTitle(title);
	        	frame.setLocation(idx * 16, idx * 4);
	        }

	        MouseAdapter mouseAdapter = new MouseAdapter() {};
	        RootPaneContainer rootPane = (RootPaneContainer)
	        		frame.getRootPane().getTopLevelAncestor();
	        glassPane = rootPane.getGlassPane();
	        glassPane.addMouseListener(mouseAdapter);
	        if (!isCommandLine || wantUI) { // Display our window!
	        	frame.setVisible(true);
	        } 

	        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
	       		    "permanentFocusOwner", new PropertyChangeListener() {
	            @Override
	            public void propertyChange(final PropertyChangeEvent e) {
	            	final Component comp = (Component) e.getNewValue();
	                if (comp instanceof JTextField) {
	                    SwingUtilities.invokeLater(new Runnable() {
	                    	@Override
	                        public void run() {
	                            JTextField textField = (JTextField) comp;
	                            textField.selectAll();
	                        }
	                    });
	                }
	            }
	        });

	        if (Persist.checkUpdts) {
	            checkUpdates.checkForUpdates();
	        }
	    } catch (Exception e) {
			System.err.println("Got error: " + e.getMessage());
			// showException(e, e.getMessage());
			e.printStackTrace();
	        System.exit(-1);
	    }
	}

	private Container createContentPane() {
	    new WorkingDir(this);

	    resizeTimer = new Timer(250, this);
	    resizeTimer.setRepeats(false);

	    contentPanel = new JPanel();
	    contentPanel.addComponentListener(this);
	    contentPanel.setLayout(new GridBagLayout());
	    contentPanel.setOpaque(true);
	    //contentPaneBGColor = contentPanel.getBackground();
	    logPanel = new LoggingPanel();

	    // Use GridBagConstraints to lay out the dialog
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
	    gbc.ipadx = 10;
	    gbc.ipady = 10;
	    gbc.insets = new Insets(5, 5, 5, 5);
	    gbc.weightx = 1.0;
	    gbc.weighty = 0.5;

	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.gridwidth = 1;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.NONE;
	    gbc.anchor = GridBagConstraints.EAST;
	    JLabel datFileLabel = new JLabel(".DAT file");
	    contentPanel.add(datFileLabel, gbc);

	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.gridwidth = 5;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.anchor = GridBagConstraints.WEST;
	    contentPanel.add(datFileTextField, gbc);
	    //        datFileTextField
	    //                .setBorder(BorderFactory.createLineBorder(Color.YELLOW));
	    datFileTextField.addMouseListener(this);

	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.gridwidth = 1;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.NONE;
	    gbc.anchor = GridBagConstraints.EAST;
	    JLabel outDirLabel = new JLabel("Output Dir  ");
	    contentPanel.add(outDirLabel, gbc);

	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.gridwidth = 4;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.anchor = GridBagConstraints.WEST;
	    contentPanel.add(outputDirTextField, gbc);
	    outputDirTextField.addMouseListener(this);

	    gbc.gridx = 5;
	    gbc.gridy = 1;
	    gbc.gridwidth = 1;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.anchor = GridBagConstraints.WEST;
	    contentPanel.add(dirViewIt, gbc);
	    dirViewIt.addActionListener(this);

	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.gridheight = 2;
	    gbc.gridwidth = 3;
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.WEST;
	    timeAxisPanel = new TimeAxisPanel(this);
	    contentPanel.add(timeAxisPanel, gbc);

	    gbc.gridx = 3;
	    gbc.gridy = 2;
	    gbc.gridheight = 1;
	    gbc.gridwidth = 3;
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.WEST;
	    csvPanel = new CsvPanel(this);
	    contentPanel.add(csvPanel, gbc);

	    gbc.gridx = 3;
	    gbc.gridy = 3;
	    gbc.gridheight = 1;
	    gbc.gridwidth = 3;
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.WEST;
	    logFilesPanel = new LogFilesPanel(this);
	    contentPanel.add(logFilesPanel, gbc);

	    gbc.gridx = 3;
	    gbc.gridy = 4;
	    gbc.gridwidth = 3;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.WEST;
	    kmlPanel = new KMLPanel(this);
	    contentPanel.add(kmlPanel, gbc);

	    gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.gridwidth = 6;
	    gbc.gridheight = 1;
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.WEST;
	    contentPanel.add(goButton, gbc);

	    gbc.gridx = 0;
	    gbc.gridy = 6;
	    gbc.gridwidth = 6;
	    gbc.gridheight = 2;
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.anchor = GridBagConstraints.WEST;
	    contentPanel.add(logPanel, gbc);

	    createEmptyBox(1, 8, gbc);
	    createEmptyBox(2, 8, gbc);
	    createEmptyBox(3, 8, gbc);
	    createEmptyBox(4, 8, gbc);

	    //contentPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
	    return contentPanel;
	}

	private void createEmptyBox(int x, int y, GridBagConstraints gbc) {
	    gbc.gridx = x;
	    gbc.gridy = y;
	    gbc.gridheight = 1;
	    gbc.gridwidth = 1;
	    gbc.insets.set(0, 0, 0, 0);
	    contentPanel.add(Box.createRigidArea(new Dimension(50, 0)), gbc);
	}

	private void getNewDatFile() {
	    if (inputFile != null) {
	        fc.setSelectedFile(inputFile);
	    }
	    try {
	        int chooserOption = fc.showOpenDialog(this);
	        if (chooserOption == JFileChooser.APPROVE_OPTION) {
	            File iFile = fc.getSelectedFile();
	            setDatFile(iFile);
	        }
	    } catch (IOException e) {
	        DatConLog.Exception(e);
	    }
	}

	private void setDatFile(File inFile) throws IOException {
	    if (DatFile.isDatFile(inFile.getAbsolutePath())
	            || DJIAssistantFile.isDJIDat(inFile)
	            || Persist.invalidStructOK) {
	        setInputFile(inFile);
	        PreAnalyze fmTask = new PreAnalyze(inputFile);
	        fmTask.execute();
	    } else {
	     	showLog("\"" + inFile.getAbsolutePath() + "\" is not a valid .DAT file!", true);
	     	setIsWorking(false);
	 	}
	}

	private void showLog(String logMsg, boolean isError) {
	 	if (logPanel == null) { // No GUI
	 		if (isError) {
	 			logMsg = "*** " + logMsg + " ***";
	 		}
		 	System.out.println(logMsg);
		} else {
			if (!isError) {
	    		logPanel.Info(logMsg);
			} else {
	    		logPanel.Error(logMsg);
			}
		}
	}

	private void createFileNames() {
	    String flyFileName = "";
	    String flyFileNameRoot = "";
	    File inputFile = new File(datFileName);
	    flyFileName = inputFile.getName();
	    flyFileNameRoot = flyFileName.substring(0, flyFileName.lastIndexOf('.'));
	    csvPanel.createFileNames(flyFileNameRoot);
	    logFilesPanel.createFileNames(flyFileNameRoot);
	    kmlPanel.createFileNames(flyFileNameRoot);
	}

	private void setFromMarkers() throws Exception {
	    if (datFile != null) {
	        timeAxisPanel.setFromMarkers(datFile);
	    }
	}

	private void go() {
	    ConvertDat convertDat = datFile.createConvertDat(this);
	    try {
	        showLog("Converting " + datFileName, false);
	        createPrintStreams();
	        setArgs(convertDat);
	        convertDat.createRecordParsers();
	        //convertDat.createSystemRecords();

	        Go go = new Go(convertDat);
	        go.execute();
	    } catch (FileBeingUsed fbu) {
	        showLog("Can't convert because " + fbu.getFileName() +
	        		" is currently being used.", true);
	    }
	}

	private void createPrintStreams() throws FileBeingUsed {
	    try {
	        csvPanel.createPrintStreams(outputDirPath);
	        logFilesPanel.createPrintStreams(outputDirPath);
	        kmlPanel.createPrintStreams(outputDirPath);
	    } catch (FileNotFoundException e) {
	        String msg = e.getMessage();
	        if (msg.indexOf("being used by another process)") > 0) {
	            String fileName = msg.substring(0, msg.indexOf(" ("));
	            throw (new FileBeingUsed(fileName));
	        }
	    }
	}

	private void closePrintStreams() {
	    csvPanel.closePrintStreams();
	    logFilesPanel.closePrintStreams();
	    kmlPanel.closePrintStreams();
	}

	private void setOutputDir(File file) {
	    outputDir = file;
	    outputDirPath = outputDir.getAbsolutePath();
	    outputDirTextField.setText(outputDirPath);
	}

	private void reset() {
	    timeAxisPanel.reset();
	    csvPanel.reset();
	    //HPElevationPanel.reset();
	    logFilesPanel.reset();
	    kmlPanel.reset();
	    //HPElevationPanel.reset();
	}

	private void setArgs(ConvertDat convertDat) {
	    timeAxisPanel.setArgs(convertDat);
	    csvPanel.setArgs(convertDat);
	    logFilesPanel.setArgs(convertDat);
	    kmlPanel.setArgs(convertDat);
	    //HPElevationPanel.setArgs(convertDat);
	}

	private void setInputFile(File inFile) {
	    inputFile = inFile;
	    String fName = inputFile.getAbsolutePath();
	    Persist.inputFileName = fName;
	    datFileTextField.setText(fName);
	    if (!isCommandLine) {
	    	setOutputDir(inputFile.getParentFile());
	    }
	}

	private void installOutputDir() {
	    Persist.outputDirName = outputDirPath;
	    Persist.save();
	    checkIOState();
	}

	private void doWaitCursor(boolean wait) {
	    glassPane.setCursor(Cursor.getPredefinedCursor(wait ? Cursor.WAIT_CURSOR
	     													: Cursor.DEFAULT_CURSOR));
	    glassPane.setVisible(wait);
	}

	public void checkIOState() {
	    String msg = "";
	    if (inputFile != null && datFileTextField.getText().length() > 0) {
	        datFileTextField.setBackground(Color.WHITE);
	    } else {
	        datFileTextField.setBackground(Color.RED);
	        msg += ".DAT file not specified,";
	    }
	    if (outputDir != null && outputDirTextField.getText().length() > 0) {
	        outputDirTextField.setBackground(Color.WHITE);
	    } else {
	        outputDirTextField.setBackground(Color.RED);
	        msg += "OutputDir not specified,";
	    }
	    if (timeAxisPanel.tickLower > timeAxisPanel.tickUpper) {
	        msg += "Lower is greater than Upper";
	    }
		// if (timeAxisPanel.gpsLockTick > 0
		// && timeAxisPanel.tickLower >= timeAxisPanel.gpsLockTick) {
		// dashwarePanel.enableDashware(true);
		// }
		// if (timeAxisPanel.gpsLockTick == -1
		// || timeAxisPanel.tickLower < timeAxisPanel.gpsLockTick) {
		// dashwarePanel.enableDashware(false);
		// }

	    if (!msg.isEmpty() || exception) {
	    	showException(new RuntimeException("Cannot parse."), msg);
	    	if (frame != null) {
	            goButton.setBackground(Color.RED);
	            goButton.setText("Cannot Go: " + msg);
	          	exception = false;
	    	}
	    }
	}

	public void dontViewIt() {
	    csvPanel.dontViewIt();
	    logFilesPanel.dontViewIt();
	    kmlPanel.dontViewIt();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    try {
	        Object source = e.getSource();
	        if (source == goButton) {
	            go();
	        } else
	        if (source == dirViewIt) {
	            Desktop.getDesktop().open(new File(outputDirPath));
	        } else
	        if (source == resizeTimer) {
	            Persist.save();
	        }
	    } catch (Exception exception) {
	        DatConLog.Exception(exception);
	    }
	}
	
	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
	    Persist.datConSize = frame.getSize();
	    if (resizeTimer.isRunning()) {
	        resizeTimer.restart();
	    } else {
	        resizeTimer.start();
	    }
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	    try {
	        JComponent source = (JComponent) e.getSource();
	        if (source == datFileTextField) {
	            getNewDatFile();
	        } else
	        if (source == outputDirTextField) {
	            if (outputDir != null)
	                dc.setSelectedFile(outputDir);
	            int returnVal = dc.showOpenDialog(this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                setOutputDir(dc.getSelectedFile());
	            }
	            installOutputDir();
	        }
	    } catch (Exception exception) {
	        DatConLog.Exception(exception);
	    }
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Go class.
	 */
	private class Go extends SwingWorker<AnalyzeDatResults, Void> {

	    private ConvertDat convertDat = null;
	    private AnalyzeDatResults results;

	    private Go(ConvertDat convertDat) {
	        this.convertDat = convertDat;
	    }

	    @Override
	    protected AnalyzeDatResults doInBackground() throws Exception {
	        doWaitCursor(true);
	        try {
	         goButton.setBackground(Color.YELLOW);
	            goButton.setText("Converting to CSV file...");
	            datFile.reset();
	            results = convertDat.analyze(true);
	        } catch (Exception e) {
	            logPanel.Error(e.getMessage() + " !!!");
	            DatConLog.Exception(e, "Cannot convert!");
	        } finally {
	            doWaitCursor(false);
	            setIsWorking(false);
	        }
	        return results;
	    }

	    @Override
	    protected void done() {
	        try {
	            super.done();
	            updateAfterGo();
	            closePrintStreams();
	          //datFile.close();
	            showLog(results.toString(), false);
	            checkIOState();
	
	            goButton.setBackground(Color.GREEN);
	            goButton.setText("Success!  (Go Again)");
	        } catch (Exception e) {
	            DatConLog.Exception(e);
	        }
	    }
	
	    private void updateAfterGo() {
	        csvPanel.updateAfterGo();
	        logFilesPanel.updateAfterGo();
	        kmlPanel.updateAfterGo(convertDat);
	    }
	
	}

	/**
	 * PreAnalyze class.
	 */
	private class PreAnalyze extends SwingWorker<Object, Object> {

	 	private File iFile = null;

	    private PreAnalyze(File iFile) {
	        this.iFile = iFile;
	    }

	    @Override
	    protected Object doInBackground() throws Exception {
	        doWaitCursor(true);
	        goButton.setBackground(Color.WHITE);
	        goButton.setForeground(Color.BLACK);
	        goButton.setText("Pre-analyzing...");
	
	        try {
	            datFile = DatFile.createDatFile(iFile.getAbsolutePath(), DatConPanel.this);

				if (datFile != null) {
	                datFile.reset();
	                datFileName = datFile.getFile().getAbsolutePath();
	                datFileTextField.setText(datFileName);
	              //inputFile = datFile.getFile();
	                Persist.save();
	                datFile.preAnalyze();
	                setFromMarkers();
	                SwingUtilities.invokeLater(new Runnable() {
	                    @Override
	                    public void run() {
	                        reset();
	                        timeAxisPanel.initFromDatFile(datFile);
	                        LogFilesPanel.instance.updateAfterPreAnalyze(datFile);
	                        DatConLog.separator();
	                        createFileNames();
	                        checkIOState();
	                        Persist.save();

	                        goButton.addActionListener(DatConPanel.this);
	 			            goButton.setEnabled(true);
	                        if (!isCommandLine) {
	                            goButton.setBackground(Color.YELLOW);
	            	            goButton.setText("Go!");
	                        } else {
	                        	goButton.doClick();
	                        }
	                    }
	                });
	            }
	        } finally {
	            doWaitCursor(false);
	        }
	        return null;
	    }

	}

}
