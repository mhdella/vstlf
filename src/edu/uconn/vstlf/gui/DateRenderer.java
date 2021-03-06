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

package edu.uconn.vstlf.gui;
import javax.swing.table.DefaultTableCellRenderer;

import java.text.SimpleDateFormat;

class DateRenderer extends DefaultTableCellRenderer {
    /**
	 * 
	 */
	private static final long serialVersionUID = 569195851434435886L;
	SimpleDateFormat formatter;
    String _dateFormat = IVstlfMain.DATE_FORMAT;
    public DateRenderer(String dateFormat)
    {
		super();
		_dateFormat = dateFormat;
    }
    public DateRenderer()
    {
		super();
		setHorizontalAlignment( CENTER );
	}

    public void setValue(Object value) {
        if (formatter==null) {
            formatter = new SimpleDateFormat(_dateFormat);
        }
        //System.out.println("Value = " + value + ", formatted Value = " + formatter.format(value));
        setText((value == null) ? "" : formatter.format(value));
    }
}