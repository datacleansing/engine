package io.datacleansing.service.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import io.datacleansing.service.representation.Flow;
import io.datacleansing.service.representation.Job;

@RestController
@RequestMapping(value= "/engine")
public class ServiceController {

	private AmazonS3 s3client;
	private RestTemplate restTemplate;

	@PostConstruct
	private void init(){
		s3client = new AmazonS3Client(new EnvironmentVariableCredentialsProvider());
		s3client.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
		restTemplate = new RestTemplate();
	}
	
	
	@CrossOrigin(origins = "*")
	@RequestMapping( method = RequestMethod.POST )
	public ResponseEntity<String> createService(
		HttpServletRequest request,
		@RequestBody Flow flow) {
		String[] jobData = restTemplate.getForEntity(flow.getJob(), Job.class).getBody().getData().split("/");
		String bucketName = jobData[3];
		String key = jobData[4];
		S3Object obj = s3client.getObject(bucketName, key);
		S3ObjectInputStream is = obj.getObjectContent();
		BufferedReader br = null;
		HashMap<String, String> schemes = new HashMap<String, String>();
		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				String[] scheme = line.split(",");
				if(scheme.length == 2){
					schemes.put(scheme[0], scheme[1]);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		StringBuffer output = new StringBuffer();
		BufferedReader sr = new BufferedReader(new StringReader(flow.getData()));
		try{
			while ((line = sr.readLine()) != null) {
				output.append(schemes.containsKey(line) ? schemes.get(line) : line);
				output.append("\n");
			}
		}catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new ResponseEntity<String>(output.toString(), HttpStatus.CREATED);
	}

}
