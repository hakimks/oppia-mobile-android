package org.digitalcampus.mobile.learning.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.digitalcampus.mobile.learning.application.DbHelper;
import org.digitalcampus.mobile.learning.application.MobileLearning;
import org.digitalcampus.mobile.learning.model.Activity;
import org.digitalcampus.mobile.learning.model.Lang;
import org.digitalcampus.mobile.learning.model.Section;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;

import com.bugsense.trace.BugSenseHandler;

public class ModuleXMLReader {

	public static final String TAG = "ModuleXMLReader";
	private Document document;
	private String tempFilePath;

	public ModuleXMLReader(String filename) {
		// TODO check that it's a valid module xml file else throw error
		File moduleXML = new File(filename);
		if (moduleXML.exists()) {

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder;
			try {
				builder = factory.newDocumentBuilder();
				document = builder.parse(moduleXML);

			} catch (ParserConfigurationException e) {
				BugSenseHandler.log(TAG, e);
				e.printStackTrace();
			} catch (SAXException e) {
				BugSenseHandler.log(TAG, e);
				e.printStackTrace();
			} catch (IOException e) {
				BugSenseHandler.log(TAG, e);
				e.printStackTrace();
			}
		}
	}
	
	public String getTempFilePath() {
		return tempFilePath;
	}

	public void setTempFilePath(String tempFilePath) {
		this.tempFilePath = tempFilePath;
	}
	
	public ArrayList<Lang> getTitles(){
		ArrayList<Lang> titles = new ArrayList<Lang>();
		Node m = document.getFirstChild().getFirstChild();
		NodeList meta = m.getChildNodes();
		for (int j=0; j<meta.getLength(); j++) {
			if(meta.item(j).getNodeName().equals("title")){
				NamedNodeMap attrs = meta.item(j).getAttributes();
				if(attrs.getNamedItem("lang") != null){
					String lang = attrs.getNamedItem("lang").getTextContent();
					titles.add(new Lang(lang, meta.item(j).getTextContent()));
				} else {
					titles.add(new Lang(MobileLearning.DEFAULT_LANG, meta.item(j).getTextContent()));
				}
			}
		}
		return titles;
	}
	
	public TreeSet<String> getLangs(){
		TreeSet<String> langs = new TreeSet<String>();
		Node m = document.getFirstChild().getFirstChild();
		NodeList meta = m.getChildNodes();
		for (int j=0; j<meta.getLength(); j++) {
			if(meta.item(j).getNodeName().equals("langs")){
				NodeList langsNode = meta.item(j).getChildNodes();
				for(int k=0; k<langsNode.getLength(); k++){
					langs.add(langsNode.item(k).getTextContent());
				}
			}
		}
		return langs;
	}
	
	public HashMap<String, String> getMeta(){
		HashMap<String, String> hm = new HashMap<String, String>();
		Node m = document.getFirstChild().getFirstChild();
		NodeList meta = m.getChildNodes();
		for (int j=0; j<meta.getLength(); j++) {
			hm.put(meta.item(j).getNodeName(), meta.item(j).getTextContent());
		}
		return hm;
	}
	public String getModuleImage(){
		// TODO must be better way to do this???
		String image = null;
		Node m = document.getFirstChild().getFirstChild();
		NodeList meta = m.getChildNodes();
		for (int j=0; j<meta.getLength(); j++) {
			if(meta.item(j).getNodeName().equals("image")){
				NamedNodeMap attrs = meta.item(j).getAttributes();
				image = attrs.getNamedItem("filename").getTextContent();
			}
		}
		return image;
	}
	
	/*
	 * This is used when installing a new module
	 * and so adding all the activities to the db
	 */
	public ArrayList<Activity> getActivities(long modId){
		ArrayList<Activity>  acts = new ArrayList<Activity>();
		Node struct = document.getFirstChild().getFirstChild().getNextSibling();
		NodeList s = struct.getChildNodes();
		for (int i=0; i<s.getLength(); i++) {
			// get the id and acts
			NamedNodeMap sectionAttrs = s.item(i).getAttributes();
			//TODO add error checking with conversion to ints
			int sectionId = Integer.parseInt(sectionAttrs.getNamedItem("order").getTextContent());
			NodeList activities = s.item(i).getLastChild().getChildNodes();
			for (int j=0; j<activities.getLength(); j++) {
				
				NamedNodeMap activityAttrs = activities.item(j).getAttributes();
				String actType = activityAttrs.getNamedItem("type").getTextContent();
				//TODO add error checking with conversion to ints
				int actId = Integer.parseInt(activityAttrs.getNamedItem("order").getTextContent());
				String digest = activityAttrs.getNamedItem("digest").getTextContent();
				Activity a = new Activity();
				a.setModId(modId);
				a.setActId(actId);
				a.setSectionId(sectionId);
				a.setActType(actType);
				a.setDigest(digest);
				acts.add(a);
			}
		}
		return acts;
	}
	
	public ArrayList<Section> getSections(int modId, Context ctx){
		ArrayList<Section> sections = new ArrayList<Section>();
		NodeList sects = document.getFirstChild().getFirstChild().getNextSibling().getChildNodes();
		DbHelper db = new DbHelper(ctx);
		for (int i=0; i<sects.getLength(); i++){
			NamedNodeMap sectionAttrs = sects.item(i).getAttributes();
			int order = Integer.parseInt(sectionAttrs.getNamedItem("order").getTextContent());
			Section s = new Section();
			s.setOrder(order);
			
			//get section titles
			NodeList nodes = sects.item(i).getChildNodes();
			ArrayList<Lang> sectTitles = new ArrayList<Lang>();
			String image = null;
			for (int j=0; j<nodes.getLength(); j++) {
				NamedNodeMap attrs = nodes.item(j).getAttributes();
				if(nodes.item(j).getNodeName().equals("title")){
					String lang = attrs.getNamedItem("lang").getTextContent();
					sectTitles.add(new Lang(lang, nodes.item(j).getTextContent()));
				} else if(nodes.item(j).getNodeName().equals("image")){
					image = attrs.getNamedItem("filename").getTextContent();
				}
			}
			s.setTitles(sectTitles);
			s.setImageFile(image);
			
			float progress = db.getSectionProgress(modId, order);
			
			s.setProgress(progress);
			//now get activities
			NodeList acts = this.getChildNodeByName(sects.item(i),"activities").getChildNodes();
			for(int j=0; j<acts.getLength();j++){
				Activity a = new Activity();
				NamedNodeMap activityAttrs = acts.item(j).getAttributes();
				a.setActId(Integer.parseInt(activityAttrs.getNamedItem("order").getTextContent()));
				NamedNodeMap nnm = acts.item(j).getAttributes();
				String actType = nnm.getNamedItem("type").getTextContent();
				String digest = nnm.getNamedItem("digest").getTextContent();
				a.setActType(actType);
				a.setModId(modId);
				a.setSectionId(order);
				
				ArrayList<Lang> actTitles = new ArrayList<Lang>();
				ArrayList<Lang> actLocations = new ArrayList<Lang>();
				ArrayList<Lang> actContents = new ArrayList<Lang>();
				NodeList act = acts.item(j).getChildNodes();
				for (int k=0; k<act.getLength(); k++) {
					NamedNodeMap attrs = act.item(k).getAttributes();
					if(act.item(k).getNodeName().equals("title")){
						String lang = attrs.getNamedItem("lang").getTextContent();
						actTitles.add(new Lang(lang, act.item(k).getTextContent()));
					} else if(act.item(k).getNodeName().equals("location")){
						String lang = attrs.getNamedItem("lang").getTextContent();
						actLocations.add(new Lang(lang, act.item(k).getTextContent()));
					} else if(act.item(k).getNodeName().equals("content")){
						String lang = attrs.getNamedItem("lang").getTextContent();
						actContents.add(new Lang(lang, act.item(k).getTextContent()));
					} else if(act.item(k).getNodeName().equals("image")){
						a.setImageFile(attrs.getNamedItem("filename").getTextContent());
					}
				}
				a.setTitles(actTitles);
				a.setLocations(actLocations);
				a.setContents(actContents);
				a.setDigest(digest);
				
				// TODO add media
				s.addActivity(a);
			}
			
			sections.add(s);
			
		}
		db.close();
		return sections;
	}
	
	private Node getChildNodeByName(Node parent, String nodeName){
		NodeList nl = parent.getChildNodes();
		for (int i=0; i<nl.getLength(); i++){
			if(nl.item(i).getNodeName().equals(nodeName)){
				return nl.item(i);
			}
		}
		return null;
	}
}
