/**HTTP Presence Sensor adjustable schedule
 *  
    Hubitat HTTP presence sensor

Allows you to set chron to stop overloading servers

Works with IP cameras. Wont fail when password requested

Setup the way you want in in driver options.

=================================================================
  v2.4.1 12/21/2022 better logging
  v2.4  11/29/2022 More options added added 
  v2.3  09/25/2022 404 and 401 errors added
  v2.2  09/21/2022 Better logging 
  v2.1.1 09/14/2021
  v2.0 09/12/2021





forked from
https://github.com/joelwetzel/Hubitat-HTTP-Presence-Sensor/blob/master/httpPresenceSensor.groovy
   
    v1.0
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
def clientVersion() {
    TheVersion="2.4.1"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

	
metadata {
    definition (name: "HTTP Presence Sensor with schedule", namespace: "tmastersmart", author: "WinnFreeNet.com", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/http_presence_sensor.groovy") {
		capability "Refresh"
		capability "Sensor"
        capability "Presence Sensor"
        capability "Configuration"
	}

preferences {
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging",  description: "Recomended low level" ,defaultValue: true, required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,     defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level",    defaultValue: false,required: true

    input name: "optionA", type: "bool", title: "Report 404 as ok", description: "File Not Found is OK" ,      defaultValue: true, required: true
	input name: "optionB", type: "bool", title: "Report 401 as ok", description: "Unauthorized request is OK" ,defaultValue: true, required: true
	input name: "optionC", type: "bool", title: "Report 500 as ok", description: "Internal Server error is OK",defaultValue: false,required: true
	input name: "optionD", type: "bool", title: "Report 403 as ok", description: "Forbidden is OK",            defaultValue: false,required: true
  
    
    
    input name: "endpointUrl" , type: "string",title: "Endpoint URL",required: true
    input name: "pollMinutes" ,	type: "number",title: "Polling Minutes",description: "Schedule to check",required: true,defaultValue: 5

}

}



def configure() {
    logging("Configure", "info")
    updated()
}
    
    
def installed () {
	logging("Install", "info")
    updated()
}


def updated () {
    loggingUpdate()
    clientVersion()
    state.tryCount = 0
    schedule("0 */${pollMinutes} * ? * *", refresh)
    logging("Updated Schedule ${pollMinutes} mins", "info")
    runIn(2, refresh)
}


def refresh() {
    logging("Checking Presence  ${state.tryCount}", "debug")
	state.tryCount = state.tryCount + 1
    
    if (state.tryCount > 3 && device.currentValue('presence') != "not present") {
        logging("Presence: [Not Present] Tries:${state.tryCount} ", "warn")
        sendEvent(name: "presence", value: "not present", descriptionText: "[Not Present] Tries:${state.tryCount} ")
    }
    
	asynchttpGet("httpGetCallback", [
		uri: endpointUrl,
        timeout: 10
	]);
}




def httpGetCallback(response, data) {
	if (response == null || response.class != hubitat.scheduling.AsyncResponse) {return}
  
    def st = response.getStatus()
    code="Unprocessed error"
    if(st == 200){code="ok"}
    if(st == 400){code="Bad Request"}
	if(st == 401){code="Unauthorized"}
    if(st == 403){code="Forbidden"}
	if(st == 404){code="File Not Found"}
    if(st == 408){code="Request Timeout"}
    if(st == 418){code="I'm a teapot"}
    if(st == 429){code="Too Many Requests"}
    if(st == 500){code="Internal Server Error"}
    if(st == 502){code="Bad Gateway"}
    if(st == 503){code="Service Unavailable"}
    if(st == 504){code="Gateway Timeout"}
    
    if(optionA && st == 404 ){state.tryCount = 0}
    if(optionB && st == 401 ){state.tryCount = 0}
    if(optionC && st == 500 ){state.tryCount = 0}
    if(optionD && st == 403 ){state.tryCount = 0}
    if (st == 200) {state.tryCount = 0}
    
    logging("Presence check [${st} ${code}] Tries:${state.tryCount}", "info") 
     if(state.tryCount == 0){
      if (device.currentValue('presence') != "present") {
      logging("Presence: [Present] Tries:${state.tryCount} ", "info")
	  sendEvent(name: "presence", value: "present", descriptionText: "[Present] ${st} ${code} Tries:${state.tryCount} ")
	  }
     }
}



// Logging block  v4

void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
}

void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}
void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}
private logging(String message, String level) {
    if (level == "infoBypass"){log.info  "${device} : $message"}
	if (level == "error"){     log.error "${device} : $message"}
	if (level == "warn") {     log.warn  "${device} : $message"}
	if (level == "trace" && traceLogging) {log.trace "${device} : $message"}
	if (level == "debug" && debugLogging) {log.debug "${device} : $message"}
    if (level == "info"  && infoLogging)  {log.info  "${device} : $message"}
}
