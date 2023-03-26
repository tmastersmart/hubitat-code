/* SmartThings motion sensor
SmartThings motion sensor for hubitat

Model:STS-IRM-250  (motionv4)
Model:STS-IRM-251  (motionv5)



motionv5 is untested and may require bat map code


motionv4 may be sending false bat reports. Works at 1.8v in testing

===================================================================================================
1.0.4    03/26/2023 Min volt setting
1.0.3    02/14/2023 
1.0.2    02/13/2023 First release beta test
=================================================================================================== 
Copyright [2023] [tmaster winnfreenet.com]

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
    TheVersion="1.0.4"
if (state.version != TheVersion){
    logging("Upgrading ! ${state.version} to ${TheVersion}", "warn")
     state.version = TheVersion
     configure() 
 }
}


metadata {

definition (name: "SmartThings Motion Sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/SmartThings_motion_sensor.groovy") {

    capability "Health Check"
	capability "Battery"
	capability "Configuration"
	capability "MotionSensor"
	capability "Initialize"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
	capability "TemperatureMeasurement"
    
    
	
command "checkPresence"
command "uninstall"
    
attribute "batteryVoltage", "string"
    
    
fingerprint inClusters:"0000,0001,0003,000F,0020,0402,0500", outClusters: "0019", model: "motionv5", manufacturer:"SmartThings" // 2017
fingerprint inClusters:"0000,0001,0003,000F,0020,0402,0500", outClusters: "0019", model: "motionv4", manufacturer:"SmartThings",profileId:"0104", endpointId:"01"//	2016 
    
  
    
}
preferences {
		
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: true,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

   input name: "pingYes",type: "bool", title: "Enable schedule Ping", description: "", defaultValue: false,required: true
   input name: "pingIt" ,type: "enum", title: "Ping Time",description: "Ping every x mins. Press config after saving",options: ["5","10","15","20","25","30"], defaultValue: "10",required:true
   input name: "tempAdj",type: "enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8", "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8","-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: 0 ,required: true  
   input name: "minVoff",type: "enum", title: "Min Voltage",description: "Using minVoltTest set the min voltage your sensor will run on", options: ["1","1.6","1.7","1.8","1.9","2","2.1","2.2","2.3","2.4","2.5","2.6"], defaultValue: "1" ,required: true  

   input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
   input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 8 ,required: true 

    }
}
// Runs after first pairing.
def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
pollHR = 10
pingIt = 30 
state.minVoltTest = 1.0  
configure()   
updated()
    
}
// Runs on reboot
def initialize(){
    logging("initialize ", "debug")
    clientVersion()    
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
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
        return
    }

    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("MAP: ${descMap}", "trace")    

     // fix parse Geting 2 formats so merge them
    if (descMap.clusterId) {descMap.cluster = descMap.clusterId}
	
	
 if (descMap.cluster == "0000" ) {
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
 }   
 	def evt = zigbee.getEvent(description)
    if (evt){logging("Event: ${evt}", "debug")} // testing   
    
 // New event detction without using clusters 
if (evt.name == "batteryVoltage"){//Event: [name:batteryVoltage, value:2.9]
        batteryVoltage = evt.value
        def  powerLast = device.currentValue("battery")
        def  batVolts  = device.currentValue("batteryVoltage")
        def  minVolts  = 2.2 
        def  maxVolts  = 3
        if(state.model =="motionv4"){minVolts  = 1.0}
    if (minVoff == "1.0"){minVolts = 1 }  
    if (minVoff == "1.6"){minVolts = 1.6 }      
    if (minVoff == "1.7"){minVolts = 1.7 }      
    if (minVoff == "1.8"){minVolts = 1.8 }      
    if (minVoff == "1.9"){minVolts = 1.9 }
    if (minVoff == "2.0"){minVolts = 2 }
    if (minVoff == "2.1"){minVolts = 2.1 }
    if (minVoff == "2.2"){minVolts = 2.2 }
    if (minVoff == "2.3"){minVolts = 2.3 }
    if (minVoff == "2.4"){minVolts = 2.4 }
    if (minVoff == "2.5"){minVolts = 2.5 }      
    if (minVoff == "2.6"){minVolts = 2.6 }         
    
        logging("bat${batteryVoltage}v batLast${batVolts}v batLast${powerLast}% MinV ${state.minVoltTest}v (${batteryVoltage} -${minVolts} / ${maxVolts} - ${minVolts})", "trace")
        
        def pct = (batteryVoltage - minVolts) / (maxVolts - minVolts)
        def roundedPct = Math.round(pct * 100)
        if (roundedPct <= 0) roundedPct = 1
            batteryPercentage = Math.min(100, roundedPct)
               logging("Battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V ", "debug")   
            if (powerLast != batteryPercentage){
            logging("Battery:${batteryPercentage}%  ${batteryVoltage}V", "info")  
            sendEvent(name: "battery", value: batteryPercentage, unit: "%")      
            sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
            }
        logging("Battery Voltage:${batteryVoltage} ${batteryPercentage}%", "info") 
        return   
        
}else if (evt.name == "temperature"){//Event: [name:temperature, value:61, unit:F, descriptionText:xxx was 61F]
        def temp = evt.value
        tempLast = device.currentValue("temperature")
        if(!tempAdj){tempAdj = 0}
        temp = (temp > 100) ? (temp - 655.35) : temp 
        temp = tempOffset ? (temp + tempOffset) : temp
        temperatureF = temp     
        Double correctNum = Double.valueOf(tempAdj) 
        if (correctNum > 0 || correctNum < 0){ 
            temperatureF = (temp + correctNum)
            temperatureF = temperatureF.round(2) 
        }        

        logging("Temp:${temperatureF}°${evt.unit} Last:${tempLast}°${evt.unit} adjust:${correctNum} [Sensor:${temp}°${evt.unit} ] raw:${evt.value}", "debug")
        logging("Temp:${temperatureF}°${evt.unit}", "info")
        if (tempLast != temperatureF){     
           logging("Temp:${temperatureF}F adjust:${correctNum}°${evt.unit} [Sensor:${temp}°${evt.unit} ]", "info")
           sendEvent(name: "temperature", value: temperatureF, unit: evt.unit)
         }  
       return     
		
}else if (evt.name){ // trap ukn events
      logging("Unknown Event: ${evt}", "warn")           
      return

        

}else if (descMap.cluster == "0500"){
    if (descMap.attrId == "0002" ) {
     value = Integer.parseInt(descMap.value, 16)
     logging("0500 IAS Zone attrId:${descMap.attrId} value:${value} #${descMap.value} ", "debug")// motion4 sent 32 when inactive
     return
    }else if (descMap.commandInt == "07") {
        logging("0500 IAS Zone attrId:${descMap.attrId} commandInt:${descMap.commandInt} data:${descMap.data}", "debug")
          if (descMap.data[0] == "00") {
              logging("IAS ZONE REPORTING CONFIG RESPONSE: ", "info")
              sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
              } else {logging("IAS ZONE REPORING CONFIG FAILED - Error Code: ${descMap.data[0]} ", "warn")}
             return
                } 
logging("0500 IAS Zone (Unknown) command:${descMap.command} data:${descMap.data}", "debug")    
return   
      
// just ignore these unknown clusters for now
}else if (descMap.cluster == "0006" || descMap.cluster == "8032" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" || descMap.cluster == "8013") {
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


// sends standard zigbee command
// motionv5 alarm1
// motionv4 alarm1 and alarm2 Likely supports tamper
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set()) {motionON1()}
    else if(status.isAlarm2Set()) {motionON2()}
	else {motionOFF()}
}

private motionON1(){
		logging("Motion 1 : Active", "info")
		sendEvent(name: "motion", value: "active", isStateChange: true)
}

private motionON2(){
		logging("Motion 2 : Active", "info")
		sendEvent(name: "motion", value: "active", isStateChange: true)
}

private motionOFF(){
		logging("Motion 0 : Inactive", "info")
        sendEvent(name: "motion", value: "inactive", isStateChange: true)
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
         state.minVoltTest = device.currentValue("batteryVoltage")    
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
//  if(state.model =="motionv4"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/motion4.jpg' >"}
  state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/motion4.jpg' >"  
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

