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

import edu.uconn.vstlf.data.message.VSTLFMessage;
public class VSTLF5mRefinementMessage extends VSTLFRealTimeMessage {
	public static VSTLFMessage.Type mtype = VSTLFMessage.Type.RTRefine5m; 

	private final Date _at;
	private final double _oldVal, _newVal;
	
	public VSTLF5mRefinementMessage(Date at, double oldVal, double newVal){
		super(mtype);
		_at = at;
		_oldVal = oldVal;
		_newVal = newVal;
	}
	
	public void visit(VSTLFNotificationCenter center){
		center.refined5MPoint(_at, _oldVal, _newVal);
	}
}
