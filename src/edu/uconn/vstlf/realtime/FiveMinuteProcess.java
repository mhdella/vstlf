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
import java.util.Date;
import java.util.logging.Level;
//import java.util.Vector;
import edu.uconn.vstlf.data.Calendar;
import edu.uconn.vstlf.data.doubleprecision.*;
import edu.uconn.vstlf.data.message.LogMessage;
import edu.uconn.vstlf.data.message.MessageCenter;
import edu.uconn.vstlf.data.message.VSTLFMessage;
import edu.uconn.vstlf.database.*;
import edu.uconn.vstlf.neuro.*;
import edu.uconn.vstlf.config.Items;

public class FiveMinuteProcess extends Thread {
	private final PCBuffer<VSTLF5MPoint> _input;
	private final MessageCenter _notif;
	//private Vector<VSTLF5MPoint> _window;
	private PowerDB _db;
	private ANNBank[] _annBanks;
	private DataFeed _dataFeed = new DataFeed();												
	private Calendar _cal;
    
    private boolean _doFilter;
	boolean _useSimDay;
	double _filterThreshold;
	private ANNFactory _annfac;
	
	FiveMinuteProcess(MessageCenter notif, PCBuffer<VSTLF5MPoint> buf, PowerDB db, ANNFactory annfac) {
		_input = buf;		
		_notif = notif;
		_db = db;
		_annBanks = new ANNBank[12];
		_cal = Items.makeCalendar();
		_annfac = annfac;

		for(int i = 0;i<12;i++){
			try{
				_annBanks[i] = _annfac.buildANNBank("anns/bank"+i+".ann");
			}catch(Exception e){
				_notif.put(new VSTLFRealTimeExceptionMessage(e));
			}
		}
		_doFilter = true;
		_filterThreshold = 200;
		_useSimDay = false;
	}
	
	public void run() {
		MessageCenter.getInstance().put(new LogMessage(Level.INFO, "FiveMinuteProcess", "run", "Five minute thread starts running"));

		VSTLF5MPoint thePoint;
		while( (thePoint = _input.consume()).getType() != VSTLFMessage.Type.EOF ) {
			if (!thePoint.isValid()) {				
				break;
			}
			Date t = thePoint.getStamp(); double v = thePoint.getValue(); int n = thePoint.getNumObs();
			_db.addLoad("raw", t, v);												//Store it in the DB
			_db.commit();
			macroFilter(t);
			_notif.put(new FiveMinMessage(t,v,n));
			doUpdate();														//Update on 1hourAgo
			doPrediction();
		}
		
		MessageCenter.getInstance().put(new LogMessage(Level.INFO, "FiveMinuteProcess", "run", "Five minute thread stops"));
	}
	
	private void checkData(Date from, Series s){
		Double nan = new Double(Double.NaN);
		double[] a = s.array(false);
		for(int i=0;i<s.length();i++){
			if(nan.equals(a[i])){
				a[i] = 0;
				_notif.put(new MessageMissing5m(_cal.addMinutesTo(from, (i+1)*5)));
			}
		}
	}
	
	private void macroFilter(Date t){
		try{
			Series older = _db.getLoad("filt", _cal.addHoursTo(t, -4), _cal.addHoursTo(t, -3));
			Series recent = _db.getLoad("raw", _cal.addHoursTo(t, -3), t);
			checkData(_cal.addHoursTo(t, -3),recent);
			Series orig = older.append(recent);
			Series out = orig;
			if(_doFilter){
				//System.out.println("Filtering "+orig.length());
				out = orig.patchSpikesLargerThan(_filterThreshold);
				//System.out.format("%s\n%s\n",orig,out);
			}
			for(int i = 36;i<=out.length();i++) {
				_db.addLoad("filt", _cal.addMinutesTo(_cal.addHoursTo(t, -4),5*i), out.element(i));
				if(out.element(i) != orig.element(i)){
					_notif.put(new VSTLF5mRefinementMessage(_cal.addMinutesTo(_cal.addHoursTo(t, -4),5*i), orig.element(i), out.element(i)));
				}
			}
			_db.commit();
		}catch(Exception e){
			_notif.put(new VSTLFRealTimeExceptionMessage(e));
			try{
				Series recent = _db.getLoad("raw", _cal.addHoursTo(t, -2), t);
				for(int i = 1;i<=recent.length();i++) 
					_db.addLoad("filt", _cal.addMinutesTo(_cal.addHoursTo(t, -2),5*i), recent.element(i));
				_db.commit();
			}catch(Exception x){
				_notif.put(new VSTLFRealTimeExceptionMessage(
						new Exception("Fatal Exception, 'filtered' Database did not accept data.")));
			}
			
		}
	}
	
	
	
	/**
	 * Calls the ANN to perform a prediction @ the 5m mark. Predict and save the prediction
	 * back in PERST. 
	 * @param stamp
	 */
	private void doPrediction() {
		try{
			Date stamp = _db.last("filt");
			// First do the prediction.
			int off = _cal.getMinute(stamp);
			//System.out.println("Predicting @@@@@@@@@@@@@@@@@@@@@@@@@@\t"+stamp);
			Series[] out = _annBanks[off/5].execute(_dataFeed.inputSetFor(stamp,_cal,_db));
			Series prev = _db.getLoad("filt", _cal.addHoursTo(stamp, -11), _cal.addHoursTo(stamp, 0));
			checkData(_cal.addHoursTo(stamp, -11),prev);
			
			Series pred = _dataFeed.denormOut(out, prev);
			_notif.put(new PredictionMessage(stamp,pred.array()));
		}
		catch(Exception e){
			_notif.put(new VSTLFRealTimeExceptionMessage(e));
		}
	}
	
	private void doUpdate(){
		try{
			//Update on the most recent entire hour
			Date stamp = _cal.addHoursTo(_db.last("filt"), -1);
			//execute
			int off = _cal.getMinute(stamp);
			_annBanks[off/5].execute(_dataFeed.inputSetFor(stamp,_cal,_db));
			//and then back propagate
			_annBanks[off/5].update(_dataFeed.targetSetFor(stamp,_cal,_db));
		}
		catch(Exception e){
			_notif.put(new VSTLFRealTimeExceptionMessage(e));
		}
		
	}
	
	
	public void useFilter(boolean on){
		_doFilter = on;
	}
	
	public void setFilterThreshold(double t){
		_filterThreshold = t;
	}
}
