/*
 * fi.helsinki.cs.iot.hub.model.enabler.JavascriptPluginHelperImpl
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

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class JavascriptPluginHelperImpl implements JavascriptPluginHelper {

	@Override
	public Plugin createPlugin(String pluginName, File file) throws PluginException {
		String script = ScriptUtils.convertFileToString(file);
		return createPlugin(pluginName, script);
	}

	@Override
	public Plugin createPlugin(String pluginName, String script) throws PluginException{
		//TODO fix that
		int mode = DuktapeJavascriptEngineWrapper.EVENT_LOOP |
				DuktapeJavascriptEngineWrapper.HTTP_REQUEST |
				DuktapeJavascriptEngineWrapper.TCP_SOCKET;
		JavascriptPlugin plugin = new JavascriptPlugin(pluginName, script, mode);
		return plugin;
	}

	@Override
	public void checkPlugin(String pluginName, String script)
			throws PluginException {
		DuktapeJavascriptEngineWrapper wrapper = 
				new DuktapeJavascriptEngineWrapper();
		try {
			boolean checked = wrapper.checkPlugin(pluginName, script);
			if (!checked) {
				throw PluginException.newJavascriptException("Javascript plugin has not passed the checkup");
			}
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	@Override
	public void checkPlugin(String pluginName, File file) throws PluginException {
		String script = ScriptUtils.convertFileToString(file);
		checkPlugin(pluginName, script);
	}

}
