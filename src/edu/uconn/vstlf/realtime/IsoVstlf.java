/************************************************************************
 MIT License

 Copyright (c) 2010 University of Connecticut

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
***********************************************************************/

package edu.uconn.vstlf.realtime;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.LinkedList;

import edu.uconn.vstlf.config.Items;
import edu.uconn.vstlf.data.*;
import edu.uconn.vstlf.data.doubleprecision.*;
import edu.uconn.vstlf.database.*;
import edu.uconn.vstlf.database.perst.*;
import edu.uconn.vstlf.database.xml.*;
import edu.uconn.vstlf.gui.IVstlfMain;
import java.util.logging.*;

/*
 * TWO HARD_CODED VALUES BELOW SHOULD BE SET DEPENDING ON THE PARTICULAR TEST YOU WOULD LIKE TO DO:
 * 
 * 	_TICK_INTERVAL is the number of milliseconds between pulses. This can be used to simulate in accelerated mode
 * 					HOWEVER, it must be set to 4000 for use with LIVE data in a production setting
 * 
 * 	_TEST_TIME is a Java Date object that represents the initial timestamp of the VSTLF Engine pulse. This can be
 * 					used to test on historical data present in data files, but for use with current, live data, should
 * 					be set to null.  Instructions to run tests from some specific dates appear below.
 * 
 */

public class IsoVstlf implements IVstlfMain, PulseAction
{	
    private Logger _logger;

	// VSTLF related objects
		
	int _TICK_INTERVAL = 4000;
	
	//////////////////////////////////////////////////
	Calendar _cal;
	///////////////////////////////////////////////////
	//Set the Pulse/Startup time.
	//Specify a historical date like ...
	
	//Jun 2 2008											run with "java -Xmx512m true .rawFiveMinuteLoad.pod .filteredFiveMinuteLoad.pod 4s_Jun2008-9.pod 5m_Jun2008-9.pod"
	//Date _TEST_TIME = _cal.newDate(2008, 5, 2, 0, 0, 0);
	
	//Live feed of current load;
	Date _TEST_TIME = null;
	////////////////////////////////////////////////////////
	
	int _OUTSTREAM_PORT = 5281;
	
	static boolean _DELETE = false;
	
	Date _prevTick;
	Date _pulseTime;
	
	VSTLFEngine _engine = null;
	String _dbName;
	String _currentDataFileName;
	String _historyDataFileName;
	GetLoadData _loadData = new GetLoadData();
	boolean _coldStart;
	String _coldStartSrcType;
	String _currDataSrcType;
	PowerDB _db;
	
	DataStream _out;
	Socket _client = null;
	
   public IsoVstlf(boolean coldstart, String dbName, String currentDataXml, String dailyDataXml) throws Exception{
	   _cal = Items.makeCalendar();
		_coldStartSrcType = dailyDataXml.substring(dailyDataXml.lastIndexOf(".")+1).toLowerCase();
		_currDataSrcType = currentDataXml.substring(currentDataXml.lastIndexOf(".")+1).toLowerCase();
		this._coldStart = coldstart;
		this._dbName = dbName;
		this._currentDataFileName = currentDataXml;
		this._historyDataFileName = dailyDataXml;	
		
	    _logger = Logger.getLogger("VSTLF logger");
		_logger.addHandler(new FileHandler("VSTLF_Headless.log"));
		_out = new DataStream(this,(OutputStream)null);
   }
   
   public void setTestTime(Date tt){
		_TEST_TIME = tt;
	}
	
	public void setClockRate(int rate){
		_TICK_INTERVAL = rate;
	}
   
	public void init() throws Exception{
		_logger.fine("initializing...");
		_logger.fine("4s stream from '"+_currentDataFileName+"'");
		_logger.fine("Initial 12hrs of 5min data from '"+_historyDataFileName+"'");
		_logger.fine("Storing aggregated 5min data in '"+_dbName+"'");
		_logger.fine("\n");
		_logger.fine("Openning new working DB...");
		setupDatabases();
		_logger.fine("\t done.");		
		// setup VSTLF engine and data feed
		if(_TEST_TIME == null)
			_TEST_TIME = _cal.now();
		else
			_logger.fine("GOING FROM "+_TEST_TIME);
		_prevTick= _cal.lastTick(300, _TEST_TIME);
		_pulseTime = _TEST_TIME;
		this._engine = new VSTLFEngine(_db);
		this._engine.setUpdateRate(_TICK_INTERVAL);
		this._engine.setMacroFilteringOn(Items.isMacroFilterOn());
		this._engine.setMicroFilteringOn(Items.isMicroFilterOn());
		this._engine.setMacroFilterThreshold(Items.getMacroSpikeThreshold());
		this._engine.setMicroFilterThreshold(Items.getMicroSpikeThreshold());
		this._engine.setMaximumDataLag(Items.getMaximumDataLag());
		Date last = _TEST_TIME;
		int tries = 0;
		do{
			if(tries>0){
				Thread.sleep(8192);
				_logger.warning("Data not recent enough.  Trying to reload");
			}
			if(_coldStartSrcType.equals("xml"))this._loadData.getData(this._historyDataFileName, false);
			if (this._coldStart) {
				_logger.fine("COLD START:  filling history raw/filtered databases from init DBs");
				LinkedList<LoadData> history;
				if(_coldStartSrcType.equals("xml"))
					history = _loadData.getHistory();
				else{
					PerstPowerDB tempDB = new PerstPowerDB(_historyDataFileName,300);
					tempDB.open();
					Date tst = tempDB.begin("load");
					_logger.fine("Init DB contains data in -\t("+tst+", "+tempDB.last("load")+"]");
					tst = _cal.addHoursTo(_prevTick, -12);
					_logger.fine("Extracting data in -\t("+tst+", "+_prevTick+"]");
					Series temp = tempDB.getLoad("load", tst, _prevTick);
					tempDB.close();
					history = new LinkedList<LoadData>();
					for(int i = 1;i<=temp.length();i++){
						history.addLast(new LoadData(temp.element(i),true,_cal.addMinutesTo(tst, 5*i)));
					}
				}
				this._loadData.fillPowerDb(history, _db);
				last = history.getLast().getDate();
			}
		}while(!last.equals(_prevTick));
		_logger.fine("HISTORY ENDS AT "+last);
		//Record a fiveMinute load history
		Series theLoad = _db.getLoad("raw", _db.begin("raw"), _db.last("raw"));
		theLoad = theLoad.reverse();
		final Calendar cal = Items.makeCalendar();
		final Date ts = cal.lastTick(4, _TEST_TIME);
		new Pulse (_engine.getUpdateRate(), this, ts);  // 250		
		_engine.startCollecting(ts);
		_engine.startProcessing();
	}

   
   boolean setupDatabases(){
	   try{
		    //Delete forecast database if it is around from last time;
		   File f = new File(_dbName);
			if(f.exists() && _DELETE) { 
				if(!f.delete()) {
					_logger.severe("???"); 
					return false;
				}
			}

			_db = new PerstPowerDB(_dbName,300);
			_db.open();
			return true;
	   }
	   catch(Exception e){
		   e.printStackTrace();
		   _logger.severe(e.toString());
		   return false;
	   }		
   }
   
   public void getNewClient(){
	   new Thread(){
		   public void run(){
			   try{
				   if(_client!=null)
					   _client.setReuseAddress(true);
				   _client = new ServerSocket(_OUTSTREAM_PORT).accept();
				   _out.setPipe(_client.getOutputStream());
			   }
			   catch(Exception e){
				   e.printStackTrace();
			   }
		   }
	   }.start();
   }

   // PulseAction implementation
	public boolean run(Date at) {
		try {
			_pulseTime = _cal.addSecondsTo(_pulseTime, 4);
			at = _pulseTime;
			LoadData currentData;
			if(this._currDataSrcType.equals("xml")){
				//currentData = this._loadData.getCurrentData();
				this._loadData.getData(this._currentDataFileName, false);
				currentData = this._loadData.getCurrentData();
			} else{
				PerstPowerDB currDB = new PerstPowerDB(_currentDataFileName,4);
				currDB.open();
				currentData = new LoadData(currDB.getLoad("load", at),true,at);
				currDB.close();
			}			
			//System.out.format("Adding a 4s observation @%s : %f\n",currentData.getDate(),currentData.getValue());
			_engine.addObservation(_out, currentData.getDate(), currentData.getValue());
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

   public static void main(String[] args) {
	   System.out.println(System.getProperty("user.dir"));
		if (args.length < 5) {
			System.out.println("java edu.uconn.gui.IsoVstlfGui <coldstart> <db> <currentDataFile> <24hrDataFile>");
			return;
		}
      try {
    	 IsoVstlf proc = new IsoVstlf(Boolean.valueOf(args[0]), args[1], args[2], args[3]);
         proc.init();
      } catch (Exception e) {
         System.err.println(e.toString());
         e.printStackTrace();
         return ;
      }
   }
}
