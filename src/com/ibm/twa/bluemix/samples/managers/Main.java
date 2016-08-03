package com.ibm.twa.bluemix.samples.managers;

import com.ibm.twa.applab.client.exceptions.InvalidRuleException;
import com.ibm.twa.applab.client.exceptions.WorkloadServiceException;

public class Main {
	public static void main(String[] args) throws InvalidRuleException, WorkloadServiceException, Exception{
		WorkloadSchedulerManager manager = new WorkloadSchedulerManager();
		manager.appCheckOrCreateProcess();
	}
}
