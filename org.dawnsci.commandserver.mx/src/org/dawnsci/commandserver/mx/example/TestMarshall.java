package org.dawnsci.commandserver.mx.example;

import java.util.Arrays;

import org.dawnsci.commandserver.mx.beans.DataCollectionBean;
import org.dawnsci.commandserver.mx.beans.SubmissionBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMarshall {

	public static void main(String[] args) throws Exception {
		
		// We want to get the JSON string for this:
		DataCollectionBean col = new DataCollectionBean("fred", "d0000000001", Arrays.asList("all"));
		SubmissionBean bean = new SubmissionBean(true);
		bean.addCollection(col);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(bean);
		
		System.out.println(jsonString);
		
		final SubmissionBean beanBack = mapper.readValue(jsonString, SubmissionBean.class);
		System.out.println("Read in equals written out = "+beanBack.equals(bean));
	}
}
