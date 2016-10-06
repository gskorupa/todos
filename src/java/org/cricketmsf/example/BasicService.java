/*
 * Copyright 2015 Grzegorz Skorupa <g.skorupa at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cricketmsf.example;

import java.io.File;
import java.util.Date;
import org.cricketmsf.Event;
import org.cricketmsf.Kernel;
import org.cricketmsf.RequestObject;
import org.cricketmsf.annotation.EventHook;
import org.cricketmsf.annotation.HttpAdapterHook;
import org.cricketmsf.in.http.HtmlGenAdapterIface;
import org.cricketmsf.in.http.HttpAdapter;
import org.cricketmsf.in.http.HttpAdapterIface;
import org.cricketmsf.in.http.ParameterMapResult;
import org.cricketmsf.in.http.StandardResult;
import org.cricketmsf.in.scheduler.SchedulerIface;
import org.cricketmsf.out.db.KeyValueCacheAdapterIface;
import org.cricketmsf.out.file.FileObject;
import org.cricketmsf.out.file.FileReaderAdapterIface;
import org.cricketmsf.out.log.LoggerAdapterIface;
import org.cricketmsf.out.script.ScriptingAdapterIface;

/**
 * EchoService
 *
 * @author greg
 */
public class BasicService extends Kernel {

    // adapterClasses
    HttpAdapterIface greeterAdapter = null;
    LoggerAdapterIface logAdapter = null;
    SchedulerIface scheduler = null;
    HtmlGenAdapterIface htmlAdapter = null;
    FileReaderAdapterIface fileReader = null;
    KeyValueCacheAdapterIface webCache = null;
    // optional
    HttpAdapterIface scriptingService = null;
    ScriptingAdapterIface scriptingEngine = null;

    @Override
    public void getAdapters() {
        // standard Cricket adapters
        logAdapter = (LoggerAdapterIface) getRegistered("LoggerAdapterIface");
        scheduler = (SchedulerIface) getRegistered("SchedulerIface");
        htmlAdapter = (HtmlGenAdapterIface) getRegistered("HtmlGenAdapterIface");
        fileReader = (FileReaderAdapterIface) getRegistered("FileReaderAdapterIface");
        webCache = (KeyValueCacheAdapterIface) getRegistered("webCache");
        // optional
        scriptingService = (HttpAdapterIface) getRegistered("ScriptingService");
        scriptingEngine = (ScriptingAdapterIface) getRegistered("ScriptingEngine");
    }
    
    @Override
    public void runInitTasks() {
    }
    
    @Override
    public void runFinalTasks() {
    }

    @Override
    public void runOnce() {
        super.runOnce();
        handleEvent(Event.logInfo("BasicService.runOnce()", "executed"));
    }
    
    @HttpAdapterHook(adapterName = "ScriptingService", requestMethod = "*")
    public Object doGetScript(Event requestEvent) {
        StandardResult r=  scriptingEngine.processRequest(requestEvent.getRequest());
        return r;
    }

    /**
     * Process requests from simple web server implementation given by
     * HtmlGenAdapter access web web resources
     *
     * @param event
     * @return ParameterMapResult with the file content as a byte array
     */
    @HttpAdapterHook(adapterName = "HtmlGenAdapterIface", requestMethod = "GET")
    public Object doGet(Event event) {
        boolean useCache = htmlAdapter.useCache();
        RequestObject request = event.getRequest();
        String filePath = fileReader.getFilePath(request);

        ParameterMapResult result = new ParameterMapResult();
        result.setData(request.parameters);

        // we can use cache if available
        FileObject fo = null;
        if (useCache) {
            try {
                fo = (FileObject) webCache.get(filePath);
            } catch (ClassCastException e) {
                fo = null;
            }
        }
        if (fo == null) {
            File file = new File(filePath);
            byte[] content = fileReader.getFileBytes(file, filePath);
            if (content.length > 0) {
                fo = new FileObject();
                fo.content = content;
                fo.modified = new Date(file.lastModified());
                fo.filePath = filePath;
                fo.fileExtension = fileReader.getFileExt(filePath);
                if(useCache) {
                    webCache.put(filePath, fo);
                }
            }
        }else{
            handleEvent(Event.logInfo("cache", "readed from cache"));
        }
        // f==null means file not found (sure - it's a shortcut)
        if (fo != null) {
            result.setCode(HttpAdapter.SC_OK);
            result.setMessage("");
            result.setPayload(fo.content);
            result.setFileExtension(fo.fileExtension);
            result.setModificationDate(fo.modified);
        } else {
            result.setCode(HttpAdapter.SC_NOT_FOUND);
            result.setMessage("file not found");
        }

        return result;
    }

    @EventHook(eventCategory = Event.CATEGORY_LOG)
    public void logEvent(Event event) {
        logAdapter.log(event);
    }
    
    @EventHook(eventCategory = Event.CATEGORY_HTTP_LOG)
    public void logHttpEvent(Event event) {
        logAdapter.log(event);
    }

    @EventHook(eventCategory = "*")
    public void processEvent(Event event) {
        if (event.getTimePoint() != null) {
            scheduler.handleEvent(event);
        } else {
            handleEvent(Event.logInfo("EchoService", event.getPayload().toString()));
        }
    }
}
