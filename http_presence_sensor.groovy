/**
    HTTP Presence Sensor with schedule
 *  HTTP Presence Sensor with adjustable schedule
    Hubitat HTTP presence sensor
    
    Polls webpages to detect if something stops working. Added detection of logion for cameras.
    Allows you to set min for chron to stop overloading servers
    

================================================================================================
  v2.1.2 08/08/2022  Added pasword falure as ok
  v2.1.1 09/14/2021
  v2.0 09/12/2021

https://github.com/tmastersmart/hubitat-code/blob/main/http_presence_sensor.groovy
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/http_presence_sensor.groovy










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

	
metadata {
    definition (name: "HTTP Presence Sensor with schedule", namespace: "tmastersmart", author: "WinnFreeNet.com", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/http_presence_sensor.groovy") {
		capability "Refresh"
		capability "Sensor"
        capability "Presence Sensor"
        capability "Configuration"
	}

	preferences {
		section {
			input (	type: "string",	name: "endpointUrl",title: "Endpoint URL",required: true)
			input (	type: "bool",name: "enableDebugLogging",title: "Enable Debug Logging?",required: true,defaultValue: false)
            input (	type: "number",name: "pollMinutes",title: "Polling Minutes",	description: "Schedule to check",required: true,defaultValue: 5)
		}
	}
}


def log(msg) {
	if (enableDebugLogging) {
		log.debug msg
	}
}

def configure() {
	log.info "${device.displayName}: Config"
    updated()
}
    
    
def installed () {
	log.info "${device.displayName}: Installed"
    updated()
}


def updated () {
	log.info "${device.displayName}: updated"
    
    state.tryCount = 0
 
//    runEvery1Minute(refresh)

    schedule("0 */${pollMinutes} * ? * *", refresh)


// schedule('0 */10 * ? * *', mymethod)
//void runEvery1Minute(String handlerMethod, Map options = null)
//void runEvery5Minutes(String handlerMethod, Map options = null)
//void runEvery10Minutes(String handlerMethod, Map options = null)
//void runEvery15Minutes(String handlerMethod, Map options = null)
//void runEvery30Minutes(String handlerMethod, Map options = null)
//void runEvery1Hour(String handlerMethod, Map options = null)   
    
    
    
    
    runIn(2, refresh)
}


def refresh() {
	log.debug "${device.displayName}: Checking Presence"

	state.tryCount = state.tryCount + 1
    
    if (state.tryCount > 3 && device.currentValue('presence') != "not present") {
        def descriptionText = "${device.displayName} is OFFLINE";
        log descriptionText
        sendEvent(name: "presence", value: "not present", linkText: deviceName, descriptionText: descriptionText)
    }
    
	asynchttpGet("httpGetCallback", [
		uri: endpointUrl,
        timeout: 10
	]);
}


def httpGetCallback(response, data) {
	
	if (response == null || response.class != hubitat.scheduling.AsyncResponse) {
		return
	}
    
    def st = response.getStatus()
    
    log.debug "${device.displayName}: Presence check status =${st}"
// 200 ok 401 pasword	
	if (st == 200 | st== 401) {
		state.tryCount = 0
		
		if (device.currentValue('presence') != "present") {
			def descriptionText = "${device.displayName} is ONLINE";
			log descriptionText
			sendEvent(name: "presence", value: "present", linkText: deviceName, descriptionText: descriptionText)
            
		}
	}
}

