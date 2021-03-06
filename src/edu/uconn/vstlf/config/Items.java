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

package edu.uconn.vstlf.config;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.uconn.vstlf.data.Calendar;

public enum Items{
	
	MinLoad("minload","5000"),
	MaxLoad("maxload","28000"),
	TimeZone("timezone","America/New_York"),
	Longitude("longitude","-72.6166667"),
	Latitude("latitude", "42.2041667"),
	DoMicroFilter("filter-micro-spikes","true"),
	DoMacroFilter("filter-macro-spikes","true"),
	MicroSpikeThreshold("micro-spike-thresh", "50"),
	MacroSpikeThreshold("macro-spike-thresh", "500"),
	MaxDataLag("max-data-delay","16"),
	DecompWindow("decomp-window", "11"),
	TestMode("testmode", "false");
	
	private static String _filename = "config.xml";

	
	private String _key;
	private String _defaultValue;
	
	private Items(String key, String defaultValue){
		_key = key;
		_defaultValue = defaultValue;
	}
	
	public String key(){
		return _key;
	}
	
	public String value(){
		return get(this);
	}
	
	public static String file(){
		return _filename;
	}
	
	private static Map<String,String> _map = new TreeMap<String,String>();      
	
	public static synchronized String get(Items item){
		if(_map.containsKey(item._key))
			return _map.get(item._key);
		else
			return item._defaultValue;
	}
	
	public static synchronized void put(Items item, String val){
		_map.put(item._key,val);
	}
	
	public static Calendar makeCalendar() {
		Calendar cal = new Calendar(Items.get(TimeZone));
		return cal;
	}
	public static double getMinimumLoad(){
		return new Double(get(Items.MinLoad));
	}
	public static double getMaximumLoad(){
		return new Double(get(Items.MaxLoad));
	}
	public static double getLatitude(){
		return new Double(get(Items.Latitude));
	}
	public static double getLongitude(){
		return new Double(get(Items.Longitude));
	}
	public static boolean isTestMode(){
		return get(Items.TestMode).toLowerCase().equals("true");
	}
	public static boolean isMicroFilterOn(){
		return get(Items.DoMicroFilter).toLowerCase().equals("true");
	}
	public static boolean isMacroFilterOn(){
		return get(Items.DoMacroFilter).toLowerCase().equals("true");
	}
	public static double getMicroSpikeThreshold(){
		return new Double(get(Items.MicroSpikeThreshold));
	}
	public static double getMacroSpikeThreshold(){
		return new Double(get(Items.MacroSpikeThreshold));
	}
	public static int getMaximumDataLag(){
		return new Integer(get(Items.MaxDataLag));
	}
	
	public static int getDecompWindow() {
		return new Integer(get(Items.DecompWindow));
	}
	
	public static synchronized void load(String filename)throws Exception{
		File file = new File(filename);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(file);
		dom.getDocumentElement().normalize();
		NodeList nodes = dom.getElementsByTagName("vstlf:item");
		for(int i = 0; i < nodes.getLength(); i++){
			Node node = nodes.item(i);
			Element elm = (Element) node;
			String key = elm.getAttribute("key");
			String val = elm.getAttribute("value");
			_map.put(key,val);
		}
	}
	
	public static synchronized void save(String filename)throws Exception{
		File dir = new File("anns");
		boolean status = false;
		if(!dir.exists())
			status = dir.mkdirs();
		if (!status) {
			Logger perstLogger = Logger.getLogger("perst");
			perstLogger.fine("Couldn't create directory [anns]");
		}
		PrintWriter out = new PrintWriter(filename);
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.println("<vstlf:config>");
		for(Items item : Items.values())
			out.format("\t<vstlf:item key=\"%s\" value=\"%s\" />\n",
						item._key,
						get(item));
		out.println("</vstlf:config>\n");
		out.close();
	}
}