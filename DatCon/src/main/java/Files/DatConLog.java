/* LoggingPanel  class

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
 */

package Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import App.DatCon;

public class DatConLog {
    private static PrintWriter dotdatPW = null;

    public DatConLog(File inFile, File outFile) {
    	// No longer using the user's home, but the file dir where we're looking anyway!
        //String userHome = System.getProperty("user.home");
        //if (userHome != null && userHome.length() > 0) {
        	String logPath = inFile.getAbsolutePath() + ".LOG";
            try {
                dotdatPW = new PrintWriter(new BufferedWriter(
                        new FileWriter(logPath, false)));
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        //}
        dotdatPW.println("Version " + DatCon.version);
        dotdatPW.flush();
    }

    public static void Log(String msg) {
        if (msg.length() > 0) {
            if (dotdatPW != null) {
                dotdatPW.println("MSG: " + msg);
                dotdatPW.flush();
            }
        }
    }

    public static void Error(String error) {
        if (dotdatPW != null) {
            dotdatPW.println("ERROR: " + error);
            dotdatPW.flush();
        }
    }

    public static void Exception(Exception e) {
        if (dotdatPW != null) {
        	dotdatPW.println(e.getMessage());
            e.printStackTrace(dotdatPW);
            dotdatPW.flush();
        }
    }

    public static void Exception(Exception e, String msg) {
        if (dotdatPW != null) {
        	if (msg != null) {
        		//DatCon.getInstance().showException(e, msg);
        		dotdatPW.println(msg);
        	}
        	e.printStackTrace(dotdatPW);
            dotdatPW.flush();
        }
    }

    public static void separator() {
        if (dotdatPW != null) {
            dotdatPW.println(
                    "########################################################\n");
            dotdatPW.flush();
        }
    }

    public boolean ok() {
        return (dotdatPW != null);
    }

}
