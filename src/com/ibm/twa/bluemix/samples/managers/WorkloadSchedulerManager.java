/*********************************************************************
 *
 * Licensed Materials - Property of IBM
 * Product ID = 5698-WSH
 *
 * Copyright IBM Corp. 2015. All Rights Reserved.
 *
 ********************************************************************/ 

package com.ibm.twa.bluemix.samples.managers;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.wink.json4j.JSONException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.twa.applab.client.WorkloadService;
import com.ibm.twa.applab.client.exceptions.InvalidRuleException;
import com.ibm.twa.applab.client.exceptions.WorkloadServiceException;
import com.ibm.tws.api.ApiClient;
import com.ibm.tws.api.ApiException;
import com.ibm.tws.api.Configuration;
import com.ibm.tws.api.ProcessApi;
import com.ibm.tws.api.ProcessLibraryApi;
import com.ibm.tws.api.auth.HttpBasicAuth;
import com.ibm.tws.model.ProcessLibrary;
import com.ibm.tws.model.RestAction;
import com.ibm.tws.model.RestAuthenticationData;
import com.ibm.tws.model.RestInput;
import com.ibm.tws.model.DailyTriggerProperty;
import com.ibm.tws.model.Process;
import com.ibm.tws.model.Step;
import com.ibm.tws.model.Trigger;
import com.ibm.tws.model.RestfulStep;

public class WorkloadSchedulerManager extends Manager{
	
    final static String workloadServiceName = "WorkloadScheduler";
    final static String processLibraryName = "wslib";
    final static String processName = "wspo";
	final static String engineName = "engine";
	final static String engineOwner = "engine";
    
	private boolean debugMode;
	private boolean processExist;
	private String processlibraryid;
	private WorkloadService ws;
	private String tenantId;
	private ProcessLibraryApi apiLibrary;
	private long myProcessId;
	private ApiClient apiClient;
	
	private String agentName = "_CLOUD";
	final static String JOB_SECTION_SEPARATOR = "===============================================================";
	
	public WorkloadSchedulerManager(){
		super(workloadServiceName);
		this.setDebugMode(false);
		
	}
	
	public void connect() {
		try {
			apiClient = Configuration.getDefaultApiClient();
			int simpleIndex = super.getUrl().indexOf("Simple") + 6;
			String url = super.getUrl().substring(0, simpleIndex);
			url += "/rest";
			apiClient.setBasePath(url);
			System.out.println(url);
			// Configure HTTP basic authorization: basicAuth
		    HttpBasicAuth basicAuth = (HttpBasicAuth) apiClient.getAuthentication("basicAuth");
		    basicAuth.setUsername(super.getUser());
		    basicAuth.setPassword(super.getPassword());
			apiLibrary = new ProcessLibraryApi();
			int index = super.getUrl().indexOf("tenantId=") + 9;
			String prefix = super.getUrl().substring(index, index + 2);
			tenantId = prefix;
			this.ws = new WorkloadService(this.getUrl());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (WorkloadServiceException e) {
			e.printStackTrace();
		}
	}

	/** 
	 * getProcessLibraryByName
	 * 
	 * @param String libName: process library name used for find the process library
	 * @return TaskLibrary
	 * 
	 * @throws WorkloadServiceException 
	 * 
	 * return a process library found by name or null
	 * @throws ApiException 
	 * 
	 */
	private ProcessLibrary getProcessLibraryByName(String libName) throws WorkloadServiceException, ApiException{
		ProcessLibrary lib = null;
		List<ProcessLibrary> libs = apiLibrary.listProcessLibrary(tenantId, engineName, engineOwner);
		for(ProcessLibrary l : libs){
			if(l.getName().equals(libName)){
				lib = l;
			}
		}
		return lib;
	}
	
	/** 
	 * getProcessByName
	 * 
	 * @param TaskLibrary library: process library where find a process by name
	 * @param String processName: process name used for find the process
	 * @return Task
	 * 
	 * @throws Exception, WorkloadServiceException 
	 * 
	 * return a process in a library found by name or null
	 * 
	 */
	public Process getProcessByName(ProcessLibrary library, String processName) throws Exception, WorkloadServiceException{
		Process process = null;
		ProcessApi api = new ProcessApi();
		List<Process> listProcess = api.listProcess(Integer.toString(library.getId()), tenantId, processName, "", engineName, engineOwner, null, null);
		for(Process t: listProcess){
			if(t.getName().equals(processName)){
				process =  t;
			}
		}	
		return process;
	}
	
	/**
	 * appCheckOrCreateProcess
	 * 
	 * Check if Workload Scheduler process exists and if not exists create it
	 * 
	 * @throws InvalidRuleException, Exception, WorkloadServiceException 
	 */
	public void appCheckOrCreateProcess() throws InvalidRuleException, Exception, WorkloadServiceException {
		if (this.isConnected()) 
		{
			this.ws.setTimezone(TimeZone.getTimeZone("America/Chicago"));
			ProcessLibrary lib = null;
			try{
				lib = this.getProcessLibraryByName(WorkloadSchedulerManager.processLibraryName);
			}
			catch(ApiException e){
				System.out.println(e.getCode());
			}
			int libId = -1;
			if(lib!=null)
				libId = lib.getId();
			if(lib == null){
				System.out.println("Library not found");
				System.out.println("Creating library...");
				lib = new ProcessLibrary();
				lib.setName(WorkloadSchedulerManager.processLibraryName);
				lib.setParentid(-1);
				ProcessLibrary result = apiLibrary.createProcessLibrary(lib, tenantId, engineName, engineOwner).get(0);
				libId = result.getId();
				System.out.println("This is my id: "+libId);
				System.out.println("ProcessLibrary "+result.getName()+" successfully created");
			} else{
				System.out.println(lib.getName() + " process library found");
			}
			
			this.processlibraryid = Integer.toString(libId);
			Process process = this.getProcessByName(lib, WorkloadSchedulerManager.processName);
			if(process == null){
				System.out.println("Process not found");
				System.out.println("Creating process...");
				
				// Create the Workload Automation Process 
				Process newProcess = new Process();
				newProcess.setName("SingleTriggerJavaSample");
				newProcess.description("Sample application that use IBM Workload Scheduler");
				newProcess.setProcesslibraryid(libId);
				newProcess.setProcessstatus(false);
				this.agentName = tenantId + "_CLOUD";
				//Create the Restful Step
		    	List<Step> steps = new ArrayList<Step>();
		        Step step = new Step();
		        RestfulStep rest = new RestfulStep();
		        RestAction restAction = new RestAction();
		        RestAuthenticationData restData = new RestAuthenticationData();
		        RestInput restInput = new RestInput();
		        restData.setUsername(super.getUser());
		        restData.setPassword(super.getPassword());
		        restInput.setInput("test");
		        restInput.setIsFile(false);
		        rest.setAgent(agentName);
				// Set the Restful URL 
				String url = "http://" + this.getAppURI() + "/api/sendemail/";
		        restAction.setUri(url);
		        restAction.setAccept("application/json");
		        restAction.setContentType("application/json");
		        restAction.setMethod("GET");
		        //Adding the RestAction to the RestfulStep
		        rest.setAction(restAction);
		        rest.setAuthdata(restData);
		        rest.setInput(restInput);
				// Add the RestfulStep to the Workload Automation Process
		        step.setRestfulStep(rest);
		        steps.add(step);
		        newProcess.setSteps(steps);
				
				newProcess.setProcesslibraryid(Integer.parseInt(processlibraryid));
				
				// Create triggers to add to the Workload Automation process
				
				//Trigger trigger = TriggerFactory.everyDayAt(23, 00);
				List<Trigger> triggers = new ArrayList<Trigger>();
				Trigger trigger = new Trigger();
				trigger.setTriggerType("DailyTrigger");
				trigger.setProcessId(newProcess.getId());
				DailyTriggerProperty dlproperty = new DailyTriggerProperty();
				dlproperty.setFrequency(1);
				dlproperty.setDailySchedule(1);
				trigger.setDailyProperty(dlproperty);
				// Add the Trigger to the Workload Automation process
				triggers.add(trigger);
				newProcess.setTriggers(triggers);

				//
				// Create and Enable the Workload Automation Process
				//
				// After being instantiated, a process has to be created and activated 
				// on the server before it can be triggered to run according 
				// to the specified schedule.
				//
				try {
					System.out.println("Creating and enabling the process");
					ProcessApi api = new ProcessApi();
					Process result = api.createProcess(newProcess, tenantId, engineName, engineOwner);
			    	this.myProcessId = result.getId();
			    	String id = Integer.toString(result.getId());
			    	api.toggleProcessStatus(id, tenantId, engineName, engineOwner);
					this.processExist = true;
				} catch (Exception e) {
					System.out.println("Could not connect complete the operation: " + e.getClass().getName() + " " + e.getMessage());
				}
			} else{
				System.out.println(process.getName() + " process found");
				this.processExist = true;
			}
		} else {
			System.out.println("Service not connected, please try again");
		}
	}
	
	public String getAppURI() {
		String uri = null;
		String vcapJSONString = System.getenv("VCAP_APPLICATION");
		if (vcapJSONString != null) {
			JsonObject app = new JsonParser().parse(vcapJSONString).getAsJsonObject();
			JsonArray uris = app.get("application_uris").getAsJsonArray();
			uri = uris.get(0).getAsString();
		}
		return uri;
	}
	
	public WorkloadService getWs() {
		return ws;
	}
	
	public void setWs(WorkloadService ws) {
		this.ws = ws;
	}

	public boolean isProcessExist() {
		return processExist;
	}
	
	public void setProcessExist(boolean processExist) {
		this.processExist = processExist;
	}
	
	public long getProcesslibraryid() {
		return Long.parseLong(processlibraryid);
	}
	
	public void setProcesslibraryid(long tasklibraryid) {
		this.processlibraryid = Long.toString(tasklibraryid);
	}
	
	public long getMyProcessId() {
		return myProcessId;
	}
	
	public void setMyProcessId(long myProcessId) {
		this.myProcessId = myProcessId;
	}
	
	public String getAgentName() {
		return agentName;
	}
	
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
	
}
