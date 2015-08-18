/*
 * fi.helsinki.cs.iot.hub.model.enabler.JavascriptPluginHelper
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
package fi.helsinki.cs.iot.hub.model.enabler;

import java.io.File;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public interface JavascriptPluginHelper {

	/**
	 * This method should return a functioning plugin ready to be configured
	 * @return
	 */
	public Plugin createPluginWithFilename(String pluginName, String filename) throws PluginException;
	public Plugin createPluginWithScript(String pluginName, String script) throws PluginException;
	public void checkPlugin(String pluginName, File file) throws PluginException;
	public void checkPlugin(String pluginName, String script) throws PluginException;
	public Plugin createPluginWithEnabler(Enabler enabler) throws PluginException;

}
