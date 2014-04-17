package org.dawnsci.commandserver.mx.example;

import java.util.Arrays;

import org.dawnsci.commandserver.core.Status;
import org.dawnsci.commandserver.mx.beans.DataCollectionBean;
import org.dawnsci.commandserver.mx.beans.DataCollectionsBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMarshall {

	public static void main(String[] args) throws Exception {
		
		// We want to get the JSON string for this:
		DataCollectionBean col = new DataCollectionBean("fred", "d0000000001", Arrays.asList("all"));
		DataCollectionsBean bean = new DataCollectionsBean();
		bean.addCollection(col);
		bean.setStatus(Status.SUBMITTED);
		bean.setPercentComplete(10);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(bean);
		
		System.out.println(jsonString);
		
		final DataCollectionsBean beanBack = mapper.readValue(jsonString, DataCollectionsBean.class);
		System.out.println("Read in equals written out = "+beanBack.equals(bean));
	}
}
