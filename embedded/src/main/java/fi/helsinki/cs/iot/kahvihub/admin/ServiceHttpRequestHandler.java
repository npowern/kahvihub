/*
 * fi.helsinki.cs.iot.kahvihub.admin.ServiceHttpRequestHandler
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
package fi.helsinki.cs.iot.kahvihub.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.basic.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.service.RunnableService;
import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceException;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;
import fi.helsinki.cs.iot.hub.model.service.ServiceManager;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class ServiceHttpRequestHandler extends HttpRequestHandler {

	private static final String TAG = "ServiceHttpRequestHandler";

	private final String uriFilter;
	private final String libFolder;
	private List<Method> methods;

	/**
	 * @param uriFilter
	 */
	public ServiceHttpRequestHandler(String libFolder, String uriFilter) {
		this.uriFilter = uriFilter;
		this.libFolder = libFolder;
		this.methods = new ArrayList<>();
		this.methods.add(Method.GET);
		this.methods.add(Method.POST);
		this.methods.add(Method.PUT);
		this.methods.add(Method.DELETE);
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.HttpRequestHandler#acceptRequest(fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method, java.lang.String)
	 */
	@Override
	public boolean acceptRequest(Method method, String uri) {
		return uri != null && uri.startsWith(uriFilter) && this.methods.contains(method);
	}

	private int getNumberOfIdentifiers(String uri) {
		String enablerUrlFilterWithSlash = uriFilter + "/";
		if (uri.startsWith(enablerUrlFilterWithSlash) && uri.length() > enablerUrlFilterWithSlash.length()) {
			return uri.substring(enablerUrlFilterWithSlash.length()).split("/").length;
		}
		else return 0;
	}

	private String getServiceName(String uri) {
		String enablerUrlFilterWithSlash = uriFilter + "/";
		if (uri.startsWith(enablerUrlFilterWithSlash) && uri.length() > enablerUrlFilterWithSlash.length()) {
			String[] arr = uri.substring(enablerUrlFilterWithSlash.length()).split("/");
			if (arr.length > 0) {
				return arr[0];
			}
		}
		return null;
	}

	private String getServiceCommand(String uri) {
		String enablerUrlFilterWithSlash = uriFilter + "/";
		if (uri.startsWith(enablerUrlFilterWithSlash) && uri.length() > enablerUrlFilterWithSlash.length()) {
			String[] arr = uri.substring(enablerUrlFilterWithSlash.length()).split("/");
			if (arr.length > 1) {
				return arr[1];
			}
		}
		return null;
	}

	private Response addNewService(String data) throws JSONException {
		JSONObject json = new JSONObject(data);
		String name = json.getString("name"); //Name of the object
		String fileLocator = json.getString("file");
		String metadata = json.optString("metadata");
		boolean bootAtStartup = json.has("bootAtStartup") && json.getBoolean("bootAtStartup");
		JSONObject config = json.optJSONObject("config");
		File file = new File(fileLocator);
		if (!file.exists() || file.isDirectory()) {
			Log.e(TAG, "Cannot use this file");
			return null;
		}
		//TODO I would not to parse the file as a proper javascript service
		try {
			ServiceManager.getInstance().checkService(name, ScriptUtils.convertFileToString(file));
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return getJsonResponseKo("SERVICE_EXCEPTION", e.getMessage());
		}

		File serviceFile = copyPluginFile(file);
		if (serviceFile != null) {
			ServiceInfo serviceInfo = IotHubDataAccess.getInstance().addServiceInfo(name, serviceFile);
			if (serviceInfo != null) {
				Service service = IotHubDataAccess.getInstance().addService(serviceInfo, name, metadata, 
						config != null ? config.toString() : null, bootAtStartup);
				if (service != null) {
					return getJsonResponseOk(service.toJSON().toString());
				}
				else {
					Log.e(TAG, "Could not make the service");
					IotHubDataAccess.getInstance().deleteServiceInfo(serviceInfo.getId());
				}
			}
			else {
				Log.e(TAG, "Adding service info " + name + " to the db did not work");
			}
		}
		return getJsonResponseKo("BAD_REQUEST", "BAD_REQUEST");
	}

	private File copyPluginFile(File file) {
		/* 
		 * TODO A number of features would need to be added, first it would be nice to have
		 * separate folders for native and javascript plugins
		 * then we need to make sure to manage different plugins version
		 * Update them if necessary (similar to apps and services)
		 */
		if (file != null && file.exists() && !file.isDirectory()) {
			try {
				String filename = file.getName();
				File nfile = new File(libFolder + filename);
				Files.copy(file.toPath(), 
						nfile.toPath(), 
						StandardCopyOption.REPLACE_EXISTING);
				return nfile;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	private Response handleGetRequest(String uri, Map<String, String> parameters, 
			String mimeType, Map<String, String> files) {
		int numberOfIdentifiers = getNumberOfIdentifiers(uri);
		if (numberOfIdentifiers == 0) {
			List<Service> services = IotHubDataAccess.getInstance().getServices();
			JSONArray jArray = new JSONArray();
			for (Service service : services) {
				try {
					JSONObject jDescription = new JSONObject(service.toJSON().toString());
					jArray.put(jDescription);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return getJsonResponseOk(jArray.toString());
		}
		else if (numberOfIdentifiers == 1) {
			//1: service name
			String serviceName = getServiceName(uri);
			Service service = IotHubDataAccess.getInstance().getService(serviceName);
			if (service != null) {
				try {
					return getJsonResponseOk(service.toJSON().toString());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return getJsonResponseKo("BAD_REQUEST", "The service " + serviceName + " could not be put to JSON");
				}
			}
			else {
				return getJsonResponseKo("BAD_REQUEST", "The service " + serviceName + " could not be found");
			}
		}
		else if (numberOfIdentifiers == 2) {
			//1: service name
			//2: start/stop
			String serviceName = getServiceName(uri);
			String serviceCommand = getServiceCommand(uri);
			Service service = IotHubDataAccess.getInstance().getService(serviceName);
			RunnableService runnableService = null;
			try {
				runnableService = ServiceManager.getInstance().getConfiguredRunnableService(service);
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return getJsonResponseKo("BAD_REQUEST", "BAD_REQUEST");
			}
			if ("start".equalsIgnoreCase(serviceCommand)) {
				if (!runnableService.isStarted()) {
					runnableService.start();
					JSONObject json = new JSONObject();
					try {
						json.put("status", serviceName + " started");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return getJsonResponseOk(json.toString());
				}
				else {
					JSONObject json = new JSONObject();
					try {
						json.put("status", serviceName + " already started");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return getJsonResponseOk(json.toString());
				}
			}
			else if ("stop".equalsIgnoreCase(serviceCommand)) {
				if (runnableService.isStarted()) {
					runnableService.stop();
					JSONObject json = new JSONObject();
					try {
						json.put("status", serviceName + " stopped");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return getJsonResponseOk(json.toString());
				}
				else {
					JSONObject json = new JSONObject();
					try {
						json.put("status", serviceName + " not running");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return getJsonResponseOk(json.toString());
				}
			}
			else {
				getJsonResponseKo("BAD_REQUEST", "BAD_REQUEST");
			}
		}
		return getJsonResponseKo("BAD_REQUEST", "BAD_REQUEST");
	}

	private Response handlePostRequest(String uri, Map<String, String> parameters, 
			String mimeType, Map<String, String> files) {
		//Log.d(TAG, "Received a post request for service");
		if (isJsonMimeType(mimeType)) {
			String data = getJsonData(Method.POST, files);
			int numberOfIdentifiers = getNumberOfIdentifiers(uri);
			if (data == null) {
				Log.e(TAG, "No JSON data has been found");
				return getJsonResponseKo("DATA NOT FOUND", "No JSON data has been found");
			}
			try {
				if (numberOfIdentifiers == 0) {
					return addNewService(data);
				}
				else if (numberOfIdentifiers == 1) {
					String serviceName = getServiceName(uri);
					Service service = IotHubDataAccess.getInstance().getService(serviceName);
					if (service != null) {
						// First I get the current service
						JSONObject jdata = new JSONObject(data);
						boolean bootAtStartup = service.bootAtStartup() || jdata.optBoolean("bootAtStartup");
						String name = jdata.optString("name") == null ? service.getName() : jdata.optString("name");
						String metadata = jdata.optString("metadata") == null ? service.getMetadata() : jdata.optString("metadata");
						String config = null;
						if (jdata.optJSONObject("config") != null) {
							config = jdata.getJSONObject("config").toString();
						}
						else if (jdata.optJSONArray("config") != null){
							config = jdata.getJSONArray("config").toString();
						}
						else {
							config = service.getConfig();
						}
						//TODO I would probably need to make a better flow there but I will just update quickly the info
						Service updatedService = IotHubDataAccess.getInstance().updateService(service, 
								name, metadata, config, bootAtStartup);
						if (updatedService != null &&
								ServiceManager.getInstance().updateRunnableService(service, updatedService) != null) {
							return getJsonResponseOk(updatedService.toJSON().toString());	
						}
						else {
							return getJsonResponseKo("BAD_REQUEST", "The service " + serviceName + " could not be updated");
						}

					}
					else {
						return getJsonResponseKo("BAD_REQUEST", "The service " + serviceName + " could not be found");
					}
				}
				else {
					Log.e(TAG, "Idenfifiers are not yet handled by the post request");
					return getJsonResponseKo("NOT_YET_IMPLEMENTED", "Idenfifiers are not yet handled by the post request");

				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			Log.w(TAG, "Non JSON post " + mimeType + " requests have not yet been implemented");
		}
		return null;
	}

	private Response handlePutRequest(String uri, Map<String, String> parameters, 
			String mimeType, Map<String, String> files) {
		Log.d(TAG, "Received a put request for service");
		return null;
	}

	private Response handleDeleteRequest(String uri, Map<String, String> parameters, 
			String mimeType, Map<String, String> files) {
		Log.d(TAG, "Received a delete request for service");
		int numberOfIdentifiers = getNumberOfIdentifiers(uri);
		if (numberOfIdentifiers == 1) {
			//Case where I want to delete the service
			String serviceName = getServiceName(uri);
			Service service = IotHubDataAccess.getInstance().getService(serviceName);
			if (service != null) {
				service = IotHubDataAccess.getInstance().deleteService(service);
				if (service != null) {
					long id = service.getServiceInfo().getId();
					IotHubDataAccess.getInstance().deleteServiceInfo(id);
					try {
						return getJsonResponseOk(service.toJSON().toString());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return getJsonResponseKo("BAD_REQUEST", "The service could not be put to JSON");
					}
				}
			}
		}
		return getJsonResponseKo("BAD_REQUEST", "Nothing was deleted");
	}

	private boolean isJsonMimeType(String mimeType) {
		return JSON_MIME_TYPE.equals(mimeType);
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.HttpRequestHandler#handleRequest(fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method, java.lang.String, java.util.Map, java.lang.String, java.util.Map)
	 */
	@Override
	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		Log.d(TAG, "Received request for service: /" + method.name() + " " + uri);
		switch (method) {
		case GET:
			return handleGetRequest(uri, parameters, mimeType, files);
		case POST:
			return handlePostRequest(uri, parameters, mimeType, files);
		case PUT:
			return handlePutRequest(uri, parameters, mimeType, files);
		case DELETE:
			return handleDeleteRequest(uri, parameters, mimeType, files);
		default:
			return getJsonResponseKo("METHOD_NOT_SUPPORTED", "Method " + method.name() + " is not supported");
		}
	}


	private Response getJsonResponseOk(String message) {
		Response response = new NanoHTTPD.Response(Status.OK, "application/json", message);
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}

	private Response getJsonResponseKo(String status, String errMessage) {
		JSONObject answer = new JSONObject();
		try {
			answer.put("status", status);
			answer.put("message", errMessage);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Response response = new NanoHTTPD.Response(Status.BAD_REQUEST, "application/json", answer.toString());
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}

	private String getAllowMethodsHeader() {
		if (methods != null && methods.size() > 0) {
			String header = methods.get(0).name();
			for (int i = 1; i < methods.size(); i++) {
				header += ", " + methods.get(i);
			}
			return header;
		} else {
			return null;
		}
	}

}
