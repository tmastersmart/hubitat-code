/* CentraLite water sensor
Iris CentraLite water sensor for hubitat

CentraLite/Iris/Smartthings water sensors for Hubitat
3315 3315-S 3315-SEU 3315-L 3315-G

Supports TAMPER PRESENCE and auto bad battery detection.




If any problems remove device from Hubitat then Reset device. 

To go back to internal drivers without removing use uninstall then change drivers.


===================================================================================================

1.0.0    12/07/2022 First release
=================================================================================================== 
Copyright [2022] [tmaster winnfreenet.com]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

	
 */

import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

def clientVersion() {
    TheVersion="1.0.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}



metadata {
    

definition (name: "Iris CentraLite water Sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/centralite-water-sensor.groovy") {

    capability "Health Check"
	capability "Battery"
	capability "Configuration"

	capability "Initialize"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
    capability "Water Sensor"
	capability "TemperatureMeasurement"
    
    
	
command "checkPresence"
command "uninstall"
    
attribute "batteryVoltage", "string"
    
		fingerprint inClusters: "0000,0001,0003,0402,0500,0020,0B05", outClusters: "0019",      manufacturer: "CentraLite", model: "3315-S",   deviceJoinName: "CentraLite Water Leak Sensor"
		fingerprint inClusters: "0000,0001,0003,0402,0500,0020,0B05", outClusters: "0019",      manufacturer: "CentraLite", model: "3315",     deviceJoinName: "CentraLite Water Leak Sensor"
		fingerprint inClusters: "0000,0001,0003,0402,0500,0020,0B05", outClusters: "0019",      manufacturer: "CentraLite", model: "3315-Seu", deviceJoinName: "CentraLite Water Leak Sensor"
		fingerprint inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0019",      manufacturer: "CentraLite", model: "3315-L",   deviceJoinName: "Iris Water Leak Sensor" 
		fingerprint inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0019",      manufacturer: "CentraLite", model: "3315-G",   deviceJoinName: "Centralite Water Leak Sensor" 
		fingerprint inClusters: "0000,0001,0003,000F,0020,0402,0500", outClusters: "0019",      manufacturer: "SmartThings",model: "moisturev4",deviceJoinName: "Smart Things Water Leak Sensor"
		fingerprint inClusters: "0000,0001,0003,0020,0402,0500",      outClusters: "0019",      manufacturer: "Samjin",     model: "water",    deviceJoinName: "smartthings-water-leak-IM6001" 
		fingerprint inClusters: "0000,0001,0003,0020,0402,0500,0B05", outClusters: "0019",      manufacturer: "Sercomm Corp.",model: "SZ-WTD03",deviceJoinName: "Sercomm Water Leak Sensor" 
		fingerprint inClusters: "0000,0001,0003,000F,0020,0500,0502", outClusters: "000A,0019", manufacturer: "frient A/S", model :"FLSZB-110", deviceJoinName: "frient Water Leak Sensor",profileId: "0104", deviceId: "0402"
}
preferences {
		
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: true,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

   input name: "pingYes",type: "bool", title: "Enable schedule Ping", description: "", defaultValue: false,required: true
   input name: "pingIt" ,type: "enum", title: "Ping Time",description: "Ping every x mins. Press config after saving",options: ["5","10","15","20","25","30"], defaultValue: "10",required:true
   input name: "tempAdj",type: "enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8", "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8","-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: 0 ,required: true  

   input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
   input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 8 ,required: true 

    }
}
def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
pollHR = 10
pingIt = 30    
configure()   
updated()
    
}
def initialize(){
    pollHR = 10
    pingIt = 30
    installed()
}


def uninstall() {// need to clear everything before manual driver change. 
  delayBetween([
    unschedule(),
    state.icon = "",
    state.donate = "",
    state.remove("minVoltTest"), 
    state.remove("presenceUpdated"),    
	state.remove("version"),
    state.remove("checkPhase"),
    state.remove("lastCheckInMin"),
    state.remove("icon"),
    state.remove("logo"),  
    state.remove("DataUpdate"),
    state.remove("lastCheckin"),
    state.remove("lastPoll"),
    state.remove("donate"),
    state.remove("model"),
    state.remove("MFR"),
    state.remove("poll"),
    state.remove("ping"),
    state.remove("tempAdj"),
    logging("Uninstalled - States removed you may now switch drivers", "info") , 
    ], 200)  
}

def updated(){
    logging("Updated ", "info")
    loggingUpdate()
    clientVersion()
}

def refresh() {
    if(state.DataUpdate){ logging("Refreshing ${state.MFR} Model:${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}

    delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0002)),// contact     
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)),// battery
    sendZigbeeCommands(zigbee.readAttribute(0x0402, 0x0000)),// temp
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0002)),// contact
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0003)),// contact     
   ], 900)  
    
}
def ping() {
    logging("Ping ", "info")
        delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)),// battery
    sendZigbeeCommands(zigbee.readAttribute(0x0402, 0x0000)),// temp
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0002)),// contact 
   ], 900) 

}


def configure() {
    logging("Config", "info")

// Set up the min volts auto adj. 
	 if (state.minVoltTest < 2.1 | state.minVoltTest > 2.45 ){ 
		state.minVoltTest= 2.45 
		logging("Min voltage set to ${state.minVoltTest}v Let bat run down to 0 for auto adj to work.", "warn")
	 }

    
  getIcons() 
  sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
 
  delayBetween([
    refresh(),
    sendZigbeeCommands(zigbee.batteryConfig()),
    sendZigbeeCommands(zigbee.configureReporting(0x0500, 0x0002, DataType.BITMAP16, 30, 3600, null)), // min30sec max3600sec =1hr
    sendZigbeeCommands(zigbee.temperatureConfig(30, 1800)),
    sendZigbeeCommands(zigbee.enrollResponse())  

   ], 900)     
 

    
    unschedule()
    
    
    if (pollYes){ 
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)    
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
    logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", "debug") 
    schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", checkPresence)	
    logging("Presence Check Every ${pollHR}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
    } 
    

    if (pingYes){    
// Schedule check in mins
	int checkEveryMinutes = 30							
	randomSixty = Math.abs(new Random().nextInt() % 60)
    logging("CHRON: ${randomSixty} 0/${pingIt} * * * ? *", "debug")     
	schedule("${randomSixty} 0/${pingIt} * * * ? *", ping)
    logging("PING every ${pingIt} mins  ", "info")   
    }
}
// IN Clusters
//   0x0000     Basic
//   0x0001     Power Configuration
//   0x0003     Identify
//   0x0B05     Diagnostics



def parse(String description) {
    logging("Parse: [${description}]", "trace")
    state.lastCheckin = now()
    checkPresence()
   
    if (description?.startsWith('enroll request')) { 
     zigbee.enrollResponse()
     return  
    }  

    
   	if (description.startsWith("zone status")) {// iaszone.ZoneStatus
		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
		processStatus(zoneStatus)
        
        if (state.MFR =="CentraLite"){//detect tamper on model 3315-G 
         if (description.startsWith("zone status 0x0024") || description.startsWith("zone status 0x0025")){tamper()}
         if (description.startsWith("zone status 0x0020") || description.startsWith("zone status 0x0021")){clearTamper()}
        }     
       return
    }
    

    
    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("MAP: ${descMap}", "trace")    

     // fix parse Geting 2 formats so merge them
    if (descMap.clusterId) {descMap.cluster = descMap.clusterId}
	

    if (descMap.cluster == "0001" & descMap.attrId == "0020"){
           def evt = zigbee.getEvent(description)
           if (evt){logging("Event: ${evt}", "debug")} // testing     
        
           powerLast = device.currentValue("battery")
           def rawValue = Integer.parseInt(descMap.value,16) 
           def batteryVoltage = rawValue / 10
        if(!state.minVoltTest){state.minVoltTest = 2.45}
          if (batteryVoltage < state.minVoltTest){
             state.minVoltTest = batteryVoltage
             logging("Min Voltage Lowered to ${state.minVoltTest}v", "info")  
          } 
        
           if (!(rawValue == 0 || rawValue == 255)) {
           def maxVolts = 3 
          //     logging("${batteryVoltage} -${state.minVoltTest} / ${maxVolts} - ${state.minVoltTest}", "trace")     
           def pct = (batteryVoltage - state.minVoltTest) / (maxVolts - state.minVoltTest)
           def roundedPct = Math.round(pct * 100)
         if (roundedPct <= 0) roundedPct = 1
            batteryPercentage = Math.min(100, roundedPct)
               logging("Battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V ", "debug")   
            if (powerLast != batteryPercentage){
            logging("Battery:${batteryPercentage}%  ${batteryVoltage}V", "info")  
            sendEvent(name: "battery", value: batteryPercentage, unit: "%")      
            sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
            }

        }
        
    }  else if (descMap.cluster == "0402" ) {
         def evt = zigbee.getEvent(description)
         if (evt){logging("Event: ${evt}", "debug")} // testing     
        
        
         if (descMap.attrInt == 0) {
        tempLast = device.currentValue("temperature")
        if(!tempAdj){tempAdj = 0}  
        def rawValue = Integer.parseInt(descMap.value,16)
        float temp = Integer.parseInt(descMap.value,16)/100   
        temp = (temp > 100) ? (temp - 655.35) : temp    
        temperatureC = temp.round(2)    
        temp = (location.temperatureScale == "F") ? ((temp * 1.8) + 32) : temp    
        temp = tempOffset ? (temp + tempOffset) : temp
	    temp = temp.round(2)     
        temperatureF = temp     
        Double correctNum = Double.valueOf(tempAdj) 
        if (correctNum > 0 || correctNum < 0){ 
            temperatureF = (temp + correctNum)
            temperatureF = temperatureF.round(2) 
        }
 //          logging("${descMap}", "warn")  
           logging("Temp:${temperatureF}F Last:${tempLast}°${location.temperatureScale} adjust:${correctNum} [Sensor:${temp}°${location.temperatureScale} ${temperatureC}°C] raw:${rawValue}", "debug")
        if (tempLast != temperatureF){     
           logging("Temp:${temperatureF}F adjust:${correctNum}°${location.temperatureScale} [Sensor:${temp}°${location.temperatureScale} ${temperatureC}°C]", "info")
           sendEvent(name: "temperature", value: temperatureF, unit: location.temperatureScale)
         }  
       return     
		}
        

        
}else if (descMap.cluster == "0000" ) {
        if (descMap.attrId== "0004" && descMap.attrInt ==4){
        logging("Manufacturer :${descMap.value}", "debug") 
        state.MFR = descMap.value 
        updateDataValue("manufacturer", state.MFR)
        state.DataUpdate = true 
        return    
        } 
        if (descMap.attrId== "0005" && descMap.attrInt ==5){
        logging("Model :${descMap.value}", "debug")
        state.model = descMap.value    
        updateDataValue("model", state.model)
        state.DataUpdate = true    
        return    
        } 

   
        

}else if (descMap.cluster == "0500"){
           def evt = zigbee.getEvent(description)
           if (evt){logging("Event: ${evt}", "debug")} // testing  
        
        if (descMap.attrId == "0002" ) {
        logging("0500 non iaszone.ZoneStatus report value:${descMap.value} data:${descMap.data}", "debug")    
        value = Integer.parseInt(descMap.value, 16)
            
        if (state.MFR =="CentraLite"){ // non iaszone.ZoneStatus report (mfr dependent codes) 
         if(value == 33 ){
             logging("state:Wet tamper:false", "info")
             clearTamper()
         }else if(value == 37) {
             logging("state:Wet tamper:true",  "info")
             tamper()
         }else if(value == 36) {
             logging("state:Dry tamper:true",  "info")
             tamper()
         }else if(value == 32) {
             logging("state:Dry tamper:false", "info")
             clearTamper()
         }else {logging("0500 Unknown value:${value}", "debug")
         }
       }     
        
        }else if (descMap.commandInt == "07") {
          if (descMap.data[0] == "00") {
                        logging("IAS ZONE REPORTING CONFIG RESPONSE: ", "info")
                        
                       sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
                    } else {logging("IAS ZONE REPORING CONFIG FAILED - Error Code: ${descMap.data[0]} ", "warn")}
                return
                }   
if ( descMap.data){
    logging("0500 IAS Zone command:${descMap.command} options:${descMap.options} data:${descMap.data}", "debug")
 return   
}
      
// just ignore these unknown clusters for now
}else if (descMap.cluster == "0500" ||descMap.cluster == "0006" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" || descMap.cluster == "8013") {
   text= ""
      if (descMap.cluster =="8001"){text="GENERAL"}
 else if (descMap.cluster =="8021"){text="BIND RESPONSE"}
 else if (descMap.cluster =="8031"){text="Link Quality"}
 else if (descMap.cluster =="8032"){text="Routing Table"}
 else if (descMap.cluster =="8013"){text="Multistate event"} 
   
   if (descMap.data){text ="${text} clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Cluster${descMap.cluster} Ignoring ${descMap.data} ${text}", "debug") 


 }  else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

    }


// sends standard zigbee command Using 1 2 not used
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() == true)  {waterON()}
    if (status.isAlarm1Set() == false) {waterOFF()}    

}
        

private waterON(){
		logging("Water : WET", "warn")
		sendEvent(name: "water", value: "wet", isStateChange: true)
}

private waterOFF(){
		logging("Water : Dry", "info")
        sendEvent(name: "water", value: "dry", isStateChange: true)
}

def tamper(){
    if (state.tamper != true){
    logging("Tamper : Detected", "warn")
	sendEvent(name: "tamper", value: "detected", isStateChange: true, descriptionText: "tamper detected v${state.version}")
    state.tamper= true
    }
}    
def clearTamper(){  
    if (state.tamper != false){
	logging("Tamper : Cleared", "info")
	sendEvent(name: "tamper", value: "clear",    isStateChange: true, descriptionText: "tamper clear v${state.version}")
    state.tamper = false    
     }    
    }        
        

def checkPresence() {
    // presence routine. v5.1 11-12-22
    // simulated 0% battery detection
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 20
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins Timeout:${checkMin} Tries:${state.tries}","debug")
    if (state.lastCheckInMin <= checkMin){ 
        state.tries = 0
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
            logging("Creating presence event: ${value}  ","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin) { 
      state.tries = state.tries + 1
      if (state.tries >=5){
        test = device.currentValue("presence")
        if (test != "not present" ){
         value = "not present"
         logging("Creating presence event: ${value}","warn")
         sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
         sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)    
         return // we dont want a ping after this or it could toggle
         }
         
     } 
       
     runIn(2,ping)
     if (state.tries <4){
         logging("Recovery in process Last checkin ${state.lastCheckInMin} min ago ","warn") 
         runIn(50,checkPresence)
     }
    }
}


void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

void getIcons(){
 if(state.MFR =="CentraLite"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/3315.jpg' >"}
  state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
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

