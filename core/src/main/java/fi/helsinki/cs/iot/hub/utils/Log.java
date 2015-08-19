/* 
* fi.helsinki.cs.iot.hub.utils.Log
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
package fi.helsinki.cs.iot.hub.utils;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class Log {
	
	private static Logger logger;
	
	public static void setLogger(Logger logger) {
		Log.logger = logger;
	}
	
	public static void d(String tag, String msg) {
		if(logger != null) logger.d(tag, msg);
	}
	
	public static void i(String tag, String msg) {
		if(logger != null) logger.i(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		if(logger != null) logger.w(tag, msg);
	}
	
	public static void e(String tag, String msg) {
		if(logger != null) logger.e(tag, msg);
	}
}
