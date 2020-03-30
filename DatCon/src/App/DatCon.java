package App;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

import Files.Persist;

public class DatCon {

	static public final String version = "3.5.0";

    /**
     * Main entry point.
     * @param args The arguments, if any.
     * @see showUsage() method.
     */
    static public void main(String[] args) {
    	new DatCon(args);
    }

    // Instance variables
	private File file0;
	private File file1;
	private boolean isCommandLine = false;
	private boolean sameDir = false;
	private boolean wantUI = false;

	public DatCon(String[] args) {
    	isCommandLine = (args.length > 0);
		if (isCommandLine) {
			scanArgs(args);
 
//	    	String dataModel = System.getProperty("sun.arch.data.model");
//			// Why do we even check this?  It's pretty standard...
//	    	if (dataModel.equals("64")) { // 64-bit arch
	    		if (file0.isDirectory()) {
	    			doDir(file0, file1);
	    		} else {
	    			doDatFile(file0, file1);
	    		}
//	    	} else { // 32-bit arch
//				SwingUtilities.invokeLater(new Runnable() {
//	           		@Override
//	            	public void run() {
//	                	DataModelDialog.createAndShowDataModelDialog();
//	            	}
//	        	});
//	    	}
		} else { // "Normal" DatCon
			doDatFile(null, null);
		}
    }

    /**
     * Shows usage for this tool by listing all the command line options.
     */
    private void showUsage() {
    	System.out.println("Usage:  java -jar DatCon.jar [options...]" +
    					   System.lineSeparator() +
    					   "  (Requires an installed JRE or JDK.)" +
    					   System.lineSeparator() +
    					   "Options:" +
   						   System.lineSeparator() +
    					   "  -i x:  x specifies the input file system path." +
   						   System.lineSeparator() +
    					   "         Can be a file path (single) or directory path (multiple)." +
   						   System.lineSeparator() +
    					   "  -o y:  y specifies the output directory path." +
   						   System.lineSeparator() +
    					   "         (If unspecified, same as using the -= option below.)" +
   						   System.lineSeparator() +
    					   "  -w:    Opens a UI window for each input file." +
   						   System.lineSeparator() +
    					   "  -=:    The output directory is the input (parent) directory.");
	}

    /**
     * Scans the argument(s) specified on the invoking command line.
     * It's already established that we have at least 1 argument.
     * @param args
     */
    private void scanArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-h") ||
				arg.equals("--help")) {
				showUsage();
				System.exit(0);
			}

			if (arg.equals("-i")) {
	        	file0 = new File(args[++i]);
	        	if (!file0.exists()) {
					System.out.println("Invalid input path specification.");
					System.exit(0);
	        	}
			} else
			if (arg.equals("-o")) {
				file1 = new File(args[++i]);
	        	if (!file1.exists()) {
					System.out.println("Invalid output path specification.");
					System.exit(0);
	        	}
			} else
			if (arg.equals("-w")) {
				wantUI = true;
			} else
			if (arg.equals("-=")) {
				sameDir = true;
			}
		}
    }

//  private DatConPanel datConPanel = null;
    private void doDatFile(File datFile, File outDir) {
    	// Because of business logic and GUI intertwined,
		//   only one process and set of SwingWorkers at a time!
//  	while (datConPanel != null && datConPanel.isWorking()) {
//  	}

    	// [Can't find a way to avoid rebuilding the GUI for each .DAT file!]
        // First set up this strictly-static class (not a good design!)
    	if (datFile != null) {
        	new Persist(datFile, outDir); // Sets up the log file for this .DAT file
    	}

        // A separate thread to pre-analyze the current .DAT file
        /*datConPanel =*/ new DatConPanel(datFile, outDir, isCommandLine, wantUI);
    }

    private void doDatFilesInDir(File iDir, File outDir) {
        String[] datNameList = null;

        if (isCommandLine) {
        	if (sameDir) {
        		outDir = iDir; // Write to the .DAT file's directory
        	}
        	FilenameFilter filter = new FilenameFilter() {
        		@Override
        		public boolean accept(File dir, String fileName) {
        			return fileName.toUpperCase().endsWith(".DAT");
        		}
        	};
        	datNameList = iDir.list(filter);
            for (int i = 0; i < datNameList.length; i++) { // If any
            	String filePath = iDir.getAbsolutePath() + File.separator + datNameList[i];
            	doDatFile(new File(filePath), outDir);
            }
    	}
    }

    private void doDir(File dir, File outDir) {
    	doDatFilesInDir(dir, outDir);

    	// Do its subdirectories, if any
    	FileFilter dirFilter = new FileFilter() {
    		@Override
    		public boolean accept(File f) {
    			return f.isDirectory();
    		}
    	};
    	File[] dirList = dir.listFiles(dirFilter);
    	for (int i = 0; i < dirList.length; i++) {
    		File subDir = dirList[i];
			doDir(subDir, sameDir ? subDir : outDir); // Recurse!
    	}
    }

}
