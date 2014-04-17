package org.dawnsci.commandserver.mx.example;

import uk.ac.diamond.scisoft.ispyb.client.Datacollection;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMarshall {

	public static void main(String[] args) throws Exception {
		
		// We want to get the JSON string for this:
		Datacollection col = new Datacollection("fred", "/dls/image1.png", "/dls/image2.png", "/dls/image3.png", "/dls/image4.png", "1", "0.1", "300", "120", "1", "35000", "0.1", "-1", "x", "1 s", "0", "0", "12", "0.004", "Hello World", "MyXstal_", "d0000000001", "bl0000000001", "s0000000001", "last week", "last week (but later)", "10000", "/dls/some_images/");
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(col);
		
		System.out.println(jsonString);
		
		final Datacollection colBack = mapper.readValue(jsonString, Datacollection.class);
		System.out.println("Read in equals written out = "+colBack.equals(col));
	}
}
