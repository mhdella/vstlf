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

package edu.uconn.vstlf.preprocessing;

import java.util.logging.Level;

import edu.uconn.vstlf.data.message.LogMessage;
import edu.uconn.vstlf.data.message.MessageCenter;
import edu.uconn.vstlf.database.PowerDB;
import edu.uconn.vstlf.realtime.PCBuffer;
import edu.uconn.vstlf.realtime.VSTLF5MPoint;

public class Store5MLoad implements Runnable {

	private PCBuffer<VSTLF5MPoint> _input;
	private PowerDB _db;
	private String [] _loadType;
	private VSTLF5MPoint _eois;

	private Thread _feedThread;
	
	public void init()
	{
		_feedThread = new Thread(this);
		_feedThread.start();
	}
	
	public Store5MLoad(PCBuffer<VSTLF5MPoint> input, PowerDB db, String [] storeLoadType,
			VSTLF5MPoint endOfInStream)
	{
		_input = input;
		_db = db;
		_loadType = storeLoadType;
		_eois = endOfInStream;
	}
	
	public void run()
	{
		_db.startTransaction();
		int k=0;
		while (true) {
			VSTLF5MPoint p = _input.consume();
			
			// End of the stream has been reached, exit the thread
			if (p == _eois) {
				break;
			}
			//System.out.format("Storing %f @ %s\n",p.getValue(),p.getStamp());
			for (int i = 0; i < _loadType.length; ++i)
				_db.addLoadNL(_loadType[i], p.getStamp(), p.getValue());
			k++;
			if (k % (12*24)==0)
				MessageCenter.getInstance().put(new LogMessage(Level.INFO,
						Store5MLoad.class.getName(), "run",
						"Load on " + p.getStamp() + " stored (" + p.getValue() + ")"));
		}
		_db.endTransaction();
	}
	
	public void join() throws InterruptedException
	{
		_feedThread.join();
	}
}
