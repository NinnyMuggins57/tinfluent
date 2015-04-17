/*
 * Copyright (C) 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted(TM), formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.server.spi.impl.anb;

import influent.server.spi.ExportDataService;
import influent.server.spi.impl.graphml.GraphMLUtil;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import oculus.aperture.spi.store.ConflictException;
import oculus.aperture.spi.store.ContentService;
import oculus.aperture.spi.store.ContentService.Document;
import oculus.aperture.spi.store.ContentService.DocumentDescriptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class AnbExportDataService implements ExportDataService {

	private final ContentService service;
	
	// The name of the CMS store we'll use for image captures
	private final static String DEFAULT_STORE = "aperture.render";
	
	@Inject
	public AnbExportDataService(
		ContentService service
	) {
		this.service = service;
	}
	
	
	
	/* (non-Javadoc)
	 * @see influent.server.spi.ExportDataService#toXMLDoc(org.json.JSONObject)
	 */
	@Override
	public String exportToXML(JSONObject jsonData) throws JSONException {
		try {
	        final ByteArrayOutputStream baoStream =  convertJSONToGraphML(jsonData);
			return baoStream.toString("UTF-8");
					
		} catch (JAXBException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failure serializing graph data with JAXB", e);
		} catch (UnsupportedEncodingException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failure converting graph data to UTF-8", e);
		}
	}
	
	
	@Override
	public DocumentDescriptor exportToXMLDoc(JSONObject JSONData, String version) throws ConflictException, JSONException {
		
		byte[] XMLdata;
		try {
			ByteArrayOutputStream baoStream = convertJSONToGraphML(JSONData);
			XMLdata = baoStream.toByteArray();
		} catch (JAXBException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failure serializing graph data with JAXB", e);
		}
		final String xmlType = "application/xml";
		
		// Store to the content service, return a URL to the image
		Document doc = service.createDocument();
		doc.setContentType(xmlType);
		doc.setDocument(XMLdata);
		
		// Store and let the content service pick the id
		return service.storeDocument(
			doc, 
			DEFAULT_STORE, 
			null, 
			null
		);
	}

	
	
	
	
	private ByteArrayOutputStream convertJSONToGraphML(JSONObject jsonData) throws JSONException, JAXBException {

		Chart chart = new Chart();
        chart.setChartItemCollection(new ChartItemCollection());
        chart.getChartItemCollection().setChartItems(new ArrayList<ChartItem>());
        
    	JSONArray columns = jsonData.getJSONArray("columns");
    	for (int i = 0; i < columns.length(); i++) {
            JSONArray files = columns.getJSONObject(i).getJSONArray("files");
        	for (int j = 0; j < files.length(); j++) {
        		if (files.get(j) instanceof JSONObject) {
                	
                	JSONObject file = files.getJSONObject(j);
                	
                	ChartItem nodeItem = new ChartItem();
                    chart.getChartItemCollection().getChartItems().add(nodeItem);

                    nodeItem.setAttrLabel(file.getString("title"));

                    nodeItem.setCIStyle(new CIStyle());
                    nodeItem.getCIStyle().setFont(new Font());
                    nodeItem.getCIStyle().getFont().setAttrPointSize(1);

                    nodeItem.setEnd(new End());
                    nodeItem.getEnd().setEntity(new Entity());
                    nodeItem.getEnd().getEntity().setAttrEntityId(file.getString("xfId"));
                    nodeItem.getEnd().getEntity().setIcon(new Icon());
                    nodeItem.getEnd().getEntity().getIcon().setIconStyle(new IconStyle());

                    nodeItem.setAttributeCollection(new AttributeCollection());
                    nodeItem.getAttributeCollection().setAttributes(new ArrayList<Attribute>());
                    
                    JSONArray links = file.getJSONArray("links");
                    for (int k = 0; k < links.length(); k++) {
                    	if (links.get(k) instanceof JSONObject) {
                    		JSONObject link = links.getJSONObject(k);
                    		
                    		ChartItem linkItem = new ChartItem();
                    		chart.getChartItemCollection().getChartItems().add(linkItem);

                    		linkItem.setLink(new Link());
                    		linkItem.getLink().setAttrEnd1Id(file.getString("xfId"));
                    		linkItem.getLink().setAttrEnd2Id(link.getString("destination"));

                    		linkItem.getLink().setLinkStyle(new LinkStyle());
                    		linkItem.getLink().getLinkStyle().setAttrType(link.getString("type"));
                    	}
                    }
        		}
        	}
    	}
        
    	JAXBContext jc = JAXBContext.newInstance(GraphMLUtil.GRAPHML_CLASSES);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, GraphMLUtil.SCHEMA_LOCATION);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        marshaller.marshal(chart, baoStream);
        
    	return baoStream;
	}
}
