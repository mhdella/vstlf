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

package edu.uconn.vstlf.database.xml;
import java.util.Date;


import java.util.LinkedList;
import java.util.Iterator;
import java.text.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public class ParseTrainingData extends ParseLoadData
{
	ParseParameters parameters = new ParseParameters();

	public ParseTrainingData()
	{
		super();
	}
	public ParseParameters getParseParameters()
	{
		return this.parameters;
	}
	public void parseData(String fileName)
		throws Exception
	{

		Document xmlDoc = DOMUtil.parse(fileName);
		NodeList nodeList = xmlDoc.getElementsByTagName("data");
		parseParameters(xmlDoc);
		for(int i=0; i<nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			NamedNodeMap attrs = node.getAttributes();
			//if(attrs.getNamedItem("id").getNodeValue().equals("ld_ne")){
				LoadData load = new LoadData();
				historyData.add(load);
				Node childNode = nodeList.item(i);
				NodeList childList = childNode.getChildNodes();
				for (int j=0; j<childList.getLength(); j++) {
					Node sub = childList.item(j);
					if (sub.getNodeName() == "value") {
						load.setValue(getLoadValue(sub));
					}
					else if (sub.getNodeName() == "time") {
						load.setDate(getLoadDate(sub));

					}
					else if (sub.getNodeName() == "quality") {
						load.setQuality(getLoadQuality(sub));
					}
				}
			//}
		}
	}
	void parseParameters(Document xmlDoc)
		throws NumberFormatException, ParseException
	{
		NodeList nodeList = xmlDoc.getElementsByTagName("parameters");
		for(int i=0; i<nodeList.getLength(); i++) {
			this.parameters = new ParseParameters();
			Node childNode = nodeList.item(i);
			NodeList childList = childNode.getChildNodes();
			for (int j=0; j<childList.getLength(); j++) {
				Node sub = childList.item(j);
				if (sub.getNodeName() == "tagName") {
					parameters.setTagName(sub.getTextContent());
				}
				else if (sub.getNodeName() == "startTime") {
					parameters.setStartTime(sub.getTextContent());

				}
				else if (sub.getNodeName() == "endTime") {
					parameters.setEndTime(sub.getTextContent());
				}
				else if (sub.getNodeName() == "resolution") {
					parameters.setResolution(sub.getTextContent());
				}
			}
		}


	}
	double getLoadValue(Node node)
		throws NumberFormatException
	{
		String nodeVal = node.getTextContent();
		return getLoadValue(nodeVal.trim());
	}
	double getLoadValue(String value)
		throws NumberFormatException
	{
		if (value.compareToIgnoreCase("Shutdown") == 0)
			value = "0.0";
		return Double.parseDouble(value.trim());
	}
	Date getLoadDate(Node node)
		throws ParseException
	{
		String nodeVal = node.getTextContent();
		return this.format.parse(nodeVal.trim());
	}

	Boolean getLoadQuality(Node node)
		throws ParseException
	{
		String nodeVal = node.getTextContent();
		return Boolean.valueOf(nodeVal.trim());
	}
	static public void main(String[] in)
	{
		if (in.length != 1 ) {
			System.out.println("java ParseTrainingData <fileName>");
			System.exit(0);
		}
		try {
			ParseTrainingData d = new ParseTrainingData();
			d.parseData(in[0]);
			System.out.println(d.getParseParameters().toString());
			LinkedList<LoadData> loadData = d.getHistory();
			Iterator<LoadData> iter = loadData.iterator();
			while (iter.hasNext()) {
				System.out.println(iter.next().toString());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}