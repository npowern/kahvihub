/*
 * fi.helsinki.cs.iot.hub.service.ServiceException
 * v0.1
 * 2015
 *
 * Copyright 2015 University of Helsinki
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at:
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 * See the License for the specific language governing permissions 
 * and limitations under the License.
 */
package fi.helsinki.cs.iot.hub.model.service;

import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class ServiceException extends JavascriptEngineException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2303727350711270941L;
	private static final String TAG = "ServiceException";
	
	public ServiceException(String message) {
		super(TAG, message);
	}

}
