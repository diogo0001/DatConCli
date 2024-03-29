package App;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

import Files.Persist;
import Files.DatFile;
import Files.CsvWriter;
import Files.ConvertDat;
import Files.AnalyzeDatResults;

public class DatCon {

	static public final String version = "3.5.0";
	// RUN WITH CLI ARGUMENTS
	// ALT+SHIFT+F10, Right, Edit

    /**
     * Main entry point.
     * @param args The arguments, if any.
     * see showUsage() method.
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
  	private boolean invalidStructOK = false;
	private boolean runScript = false;
	private float sampleRate = 20;
	private float totalExcecTime = 0;

	public DatCon(String[] args) {
    	isCommandLine = (args.length > 0);
		if (isCommandLine) {
			scanArgs(args);

			// TODO: Criar a  pasta "outputs" aqui
			if(sameDir || file1 == null){
//				if(!sameDir){
					if (file0.isDirectory()){
						file1 = file0;
					} else {
						file1 = new File(file0.getParent());
					}
//				} else {
//					String output = file0.getParent()+"\\output";
//					file1 = new File(output);
//					if(!file1.exists()){
//						file1.mkdir();
//						System.out.println("Output folder created: "+output);
//					}else{
//						System.out.println("Output folder: "+output);
//					}

//				}
			}

			try {
				if (file0.isDirectory()) {
					doDir(file0, file1);
				} else {
					doDatFile(file0, file1);
				}
			}catch (Exception e){
				System.err.println("Conversion errors: " + e.getMessage());
			}

			System.out.println("Total excecution time: "+ totalExcecTime+" s\n");

			// todo: timeout
			if (runScript) {
				try {
					System.out.println("Running script ...");
					// Executar script python aqui


					String scriptFolder = file0.getParent();
					if(file0.isDirectory()){
						scriptFolder = file0.toString();
					}

					System.out.println(scriptFolder+"\n"+file1.toString());


					String[] cmd = {"python", scriptFolder+"\\datlog_data.py",file1.toString()};

					Process process = Runtime.getRuntime().exec(cmd);
					BufferedReader read = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String bufferStr = null;

					while ((bufferStr = read.readLine()) != null) {
						System.out.println(bufferStr);
					}
					System.out.println("\nScript finished.");
				} catch (Exception e){
					System.out.println("\nRun script error!");
					e.printStackTrace();
				}
			}

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
    					   "  (Requires an installed JRE or JDK. v8)" +
    					   System.lineSeparator() +
    					   "Options:" +
   						   System.lineSeparator() +
    					   "  -i <x>:\\t<x> specifies the input file system path." +
   						   System.lineSeparator() +
    					   "         Can be a file path (single) or directory path (multiple)." +
   						   System.lineSeparator() +
    					   "  -o <y>:\\t<y> specifies the output directory path." +
   						   System.lineSeparator() +
    					   "         (If unspecified, same as using the -= option below.)" +
   						   System.lineSeparator() +
						   "  -invalidStructOK:    Allow parsing invalid struct files." +
						   System.lineSeparator() +
				           "  -=:\\tThe output directory is the input (parent) directory."+
						   System.lineSeparator() +
						   "  -s <sample rate>: Sample rate of log ticks - Defaut: 20 Hz"+
				           System.lineSeparator() +
				           "  -runscript  Execute python script to generate the 'processed.csv' file");
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
			} else
			if (arg.equals("-invalidStructOK")) {
				invalidStructOK = true;
			}else
			if(arg.equals("-runscript")){
				runScript = true;
			}else
			if(arg.equals("-s")){
				int index = ++i;
				if(index<args.length) {
					sampleRate = Float.parseFloat(args[index]);
				}
				else{
					System.out.println("Invalid sample rate");
				}
			}
			else{
				System.out.println("Invalid arguments.");
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
          Persist.invalidStructOK = invalidStructOK;
    	}

		long startTime = System.currentTimeMillis();

		try {
			DatFile datFileObj = DatFile.createDatFile(datFile.getAbsolutePath(), null);
			// DatFile datFileObj = new DatFile(null, datFile);

			String csv = datFile.getName();
			String[] csvname = csv.split("[.]");

			if(csvname.length > 0){
				csv = csvname[0];
			}

			System.out.println("\nFile: "+datFile.getName());
			System.out.println("Running Preanalyze ...");
			datFileObj.preAnalyze();
			System.out.println("Preanalyze done!");

			ConvertDat convertDat = datFileObj.createConvertDat(null);
			convertDat.setSampleRate(sampleRate);
			convertDat.setCsvWriter(new CsvWriter(outDir.getAbsolutePath() + "/" + csv + ".csv"));
			convertDat.createRecordParsers();

			datFileObj.reset();

			AnalyzeDatResults results = convertDat.analyze(true);

//			System.exit(-1);

		} catch (Exception e) {
			System.err.println("File conversion error: " + e.getMessage()+"\nFile: "+datFile.getName());
			// showException(e, e.getMessage());
			e.printStackTrace();

//			System.exit(-1);
		}

		long endTime = System.currentTimeMillis();
		totalExcecTime += (endTime - startTime)/1000.0;
		System.out.println("Excecution time: "+(endTime - startTime)/1000.0 +" s");
		System.out.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}

    private void doDatFilesInDir(File iDir, File outDir) {
        String[] datNameList = null;

		if (isCommandLine) {
//        	if (sameDir) {
//        		outDir = iDir; // Write to the .DAT file's directory
//        	}
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
