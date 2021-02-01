/* ConvertDat class

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

package Files.V3;

import java.io.IOException;
import java.util.Vector;

import App.DatConPanel;
import DatConRecs.Dictionary;
import DatConRecs.Payload;
import DatConRecs.Record;
import DatConRecs.RecDef.RecordDef;
import Files.AnalyzeDatResults;
import Files.ConvertDat;
import Files.Corrupted;
import Files.DatConLog;
import Files.FileEnd;
import Files.Persist;
import Files.RecSpec;

public class ConvertDatV3 extends ConvertDat {

    public ConvertDatV3(DatConPanel datCon, DatFileV3 datFile) {
        super(datCon, datFile);
    }

    public ConvertDatV3 createConvertDat(DatConPanel datCon, DatFileV3 datFile) {
        return new ConvertDatV3(datCon, datFile);
    }

    @Override
    public DatFileV3 getDatFile() {
        return (DatFileV3) _datFile;
    }

    public AnalyzeDatResults analyze(boolean printVersion) throws IOException {

        System.out.println("Converting ...");

        insertFWDateStr();
        boolean processedPayload = false;
        this.printVersion = printVersion;
        final int sampleSize = (int) (_datFile.getClockRate() / sampleRate);
        boolean gotException = false;

        try {
            _datFile.reset();
            // If there is a .csv being produced, go ahead and output the header (first row)
            if (csvWriter == null) {
                System.err.println("csvwriter null");
            }
            if (csvWriter != null) {
                csvWriter.print("Tick#,offsetTime");
                printCsvLine(lineType.HEADER);
            }
            long lastTickNoPrinted = -sampleSize;

            DatFileV3 datFileV3 = (DatFileV3) _datFile;
            //while (datFileV3.getNextDatRec(true, true, true, true)) {
            while (datFileV3.getNextDatRec(true, true, true, false)) {
                int payloadType = _datFile._type;
                long payloadStart = datFileV3._start;
                int payloadLength = datFileV3._payloadLength;
                tickNo = _datFile._tickNo;

                if (tickNo > tickRangeUpper) {
                    throw new FileEnd();
                }
                for (int i = 0; i < records.size(); i++) {
                	Record rec = (Record) records.get(i);
                    if (rec.isId(payloadType)) {
                        Payload payload = new Payload(_datFile, payloadStart,
                                payloadLength, payloadType, tickNo);
                        try {
                            rec.process(payload);
                            processedPayload = true;
                        } catch (Exception e) {
                            String errMsg = "Can't process record "
                                    + rec + " tickNo="
                                    + tickNo + " filePos=" + _datFile.getPos();
                            if (Persist.EXPERIMENTAL_DEV) {
                                System.err.println(errMsg);
                                e.printStackTrace();
                            } else {
                                DatConLog.Exception(e, errMsg);
                            }
                            throw new RuntimeException(errMsg);
                        }
                    }
                }
                if (tickRangeLower <= tickNo) {
                    // if some payloads in this tick#Group were processed
                    // then output the .csv line
                    if ((csvWriter != null) && processedPayload && tickNo >= lastTickNoPrinted + sampleSize) {

                        csvWriter.print(tickNo + "," + _datFile.timeString(tickNo, timeOffset));
                        printCsvLine(lineType.LINE);

                        lastTickNoPrinted = tickNo;
                        processedPayload = true;

                    }
                }
            }
        } catch (Corrupted e) {
        	String msg = ".DAT corrupted";
            gotException = true;
            System.err.println("Conversion Error!\n"+e.toString());
            _datCon.showException(e, msg);
            throw new RuntimeException(msg);	
        } catch (FileEnd e) {
        } catch (Exception e) {
            gotException = true;
            System.err.println("Conversion Error!\n"+e.toString());
            _datCon.showException(e, null);
        } finally {
            _datFile.close();

            String msg = "TotalNumRecExceptions:  " + Record.totalNumRecExceptions;
            System.out.println(msg);
            if (!gotException) {
	            double ratio;
	            ratio = (double) Math.round(_datFile.getErrorRatio(Corrupted.Type.CRC) * 100) / 100;
	            msg = "CRC   Error Ratio    :  " + ratio;
                System.out.println(msg);
	            ratio = (double) Math.round(_datFile.getErrorRatio(Corrupted.Type.Other) * 100) / 100;
	            msg = "Other Error Ratio    :  " + ratio;
                System.out.println(msg);
                System.out.println("Conversion Done!");
            }
            else{
                System.out.println("Conversion Fail!\n");
            }
        }
        return _datFile.getResults();
    }

    protected void insertFWDateStr() {
        addAttrValuePair("Firmware Date", _datFile.getFirmwareDate());
        addAttrValuePair("ACType", _datFile.getACTypeString());
    }

    @Override
    protected Vector<Record> getRecordInst(RecSpec recInDat) {
        Vector<Record> retv = new Vector<Record>();
        Record rec = null;
        rec = Dictionary.getRecordInst(DatConRecs.String.Dictionary.entries,
                recInDat, this, true);

        if (rec != null) {
            retv.add(rec);
            return retv;
        }

        switch (Persist.parsingMode) {
        case DAT_THEN_ENGINEERED:
            rec = getRecordInstFromDat(recInDat);
            if (rec != null) {
                retv.add(rec);
            } else {
                rec = getRecordInstEngineered(recInDat);
                if (rec != null) {
                    retv.add(rec);
                }
            }
        case ENGINEERED_THEN_DAT:
            rec = getRecordInstEngineered(recInDat);
            if (rec != null) {
                retv.add(rec);
            } else {
                rec = getRecordInstFromDat(recInDat);
                if (rec != null) {
                    retv.add(rec);
                }
            }
            break;
        case JUST_DAT:
            rec = getRecordInstFromDat(recInDat);
            if (rec != null) {
                retv.add(rec);
            }
            break;
        case JUST_ENGINEERED:
            rec = getRecordInstEngineered(recInDat);
            if (rec != null) {
                retv.add(rec);
            }
            break;
        case ENGINEERED_AND_DAT:
            rec = getRecordInstEngineered(recInDat);
            if (rec != null) {
                retv.add(rec);
            }
            rec = getRecordInstFromDat(recInDat);
            if (rec != null) {
                retv.add(rec);
            }
            break;
        default:
            return retv;
        }


        return retv;
        //        switch (Persist.parsingMode) {
        //        case DAT_THEN_ENGINEERED:
        //            retv = getRecordInstFromDat(recInDat);
        //            if (retv != null) {
        //                return retv;
        //            }
        //            return getRecordInstEngineered(recInDat);
        //        case ENGINEERED_THEN_DAT:
        //            retv = getRecordInstEngineered(recInDat);
        //            if (retv != null) {
        //                return retv;
        //            }
        //            return getRecordInstFromDat(recInDat);
        //        case JUST_DAT:
        //            return getRecordInstFromDat(recInDat);
        //        case JUST_ENGINEERED:
        //            return getRecordInstEngineered(recInDat);
        //        default:
        //            return null;
        //        }
    }

    private Record getRecordInstEngineered(RecSpec recInDat) {
        Record retv = null;
        retv = Dictionary.getRecordInst(DatConRecs.Dictionary.entries,
                recInDat, this, true);
        if (retv != null) {
            return retv;
        }
        retv = Dictionary.getRecordInst(
                DatConRecs.Created4V3.Dictionary.entries, recInDat, this,
                true);
        return retv;
    }

    private Record getRecordInstFromDat(RecSpec recInDat) {
        Vector<RecordDef> recordDefs = ((DatFileV3) _datFile).getRecordDefs();
        if (recordDefs != null) {
            for (int i = 0; i < recordDefs.size(); i++) {
                RecordDef recDef = recordDefs.get(i);
                if (recDef.getId() == recInDat.getId()
                        && recDef.getLength() <= recInDat.getLength()) {
                    recDef.init(this);
                    return recDef;
                }
            }
        }
        Record retv = null;
        if (null != (retv = Dictionary.getRecordInst(
                DatConRecs.FromOtherV3Dats.Dictionary.entries, recInDat,
                this, false))) {
            return retv;
        }

        return Dictionary.getRecordInst(
                DatConRecs.FromViewer.Dictionary.entries, recInDat, this,
                false);
    }
}
