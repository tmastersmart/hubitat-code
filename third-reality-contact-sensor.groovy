/* Third Reality Contact Sensor for Hubitat

A Third Reality Contact Sensor Driver For Hubitat

manufacturer: Third Reality, Inc
model: 3RDS17BZ
softwareBuild: Ver:1.2.3



This device gives full bat voltage as 3.5v which .5v to high.
===================================================================================================
v1.2.4  10/22/2024 Converted to Contact sensor
v1.2.3  03/25/2023 added low bat setting
v1.2.2  01/29/2023 Changed anti dupe routines to adding State change.
v1.2.1  01/23/2023 Power up init rewriten
v1.2.0  12/14/2022 Bat code rewrite
v1.1.0  12/11/2022 working
v1.0.0  12/10/2022 First release
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
    TheVersion="1.2.4"
if (state.version != TheVersion){
    logging("Upgrading ! ${state.version} to ${TheVersion}", "warn")
     state.version = TheVersion
     configure() 
 }
}

metadata {

definition (name: "Third Reality Contact Sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/third-reality-contact-sensor.groovy") {
    capability "Health Check"
	capability "Battery"
	capability "Configuration"
	capability "Initialize"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
	capability "Contact Sensor"
    
command "checkPresence"
command "uninstall"
attribute "batteryVoltage", "string"
    fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0500", outClusters:"0019", model:"3RDS17BZ", manufacturer:"Third Reality, Inc", application:"30"
}
preferences {
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: true,required: true
    input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

    input name: "pingYes",type: "bool", title: "Enable schedule Ping", description: "", defaultValue: false,required: true
    input name: "pingIt" ,type: "enum", title: "Ping Time",description: "Ping every x mins. Press config after saving",options: ["5","10","15","20","25","30"], defaultValue: "10",required:true
   input name: "minVoff",type: "enum", title: "Min Voltage",description: "Using minVoltTest set the min voltage your sensor will run on", options: ["1","1.6","1.7","1.8","1.9","2","2.1","2.2","2.3","2.4","2.5","2.6"], defaultValue: "2.2" ,required: true  

    input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
    input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 8 ,required: true 
    }
}
def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
pollHR = 10
pingIt = 30 
state.minVoltTest = 2.1   
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
    state.remove("tempOffset"),
    logging("Uninstalled - States removed you may now switch drivers", "info") , 
    ], 200)  
}

def updated(){
    logging("Updated ", "info")
    loggingUpdate()
    clientVersion()
}

def refresh() {
    if(state.MFR){ logging("Refreshing ${state.MFR} Model:${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}

    delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0002)),// contact     
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)),// battery
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0002)),// contact
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0003)),// contact      
   ], 900)  
    
}


def ping() {
    logging("Ping ", "info")
        delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)),// battery
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
    sendZigbeeCommands(zigbee.enrollResponse()),
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

    if (descMap.cluster == "0001" & descMap.attrId == "0020"){
         def  powerLast = device.currentValue("battery")
         def  batVolts  = device.currentValue("batteryVoltage")
        
         def  minVolts  = 2.1 
         def  maxVolts  = 3.2
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
        
         def rawValue = Integer.parseInt(descMap.value,16)
         if (rawValue == 0 || rawValue == 255) {return} 
         def batteryVoltage = rawValue / 10
        
        
        logging("value:${rawValue} bat${batteryVoltage}v batLast${batVolts}v batLast${powerLast}% MinV ${state.minVoltTest}v", "trace")

        logging("${batteryVoltage} -${minVolts} / ${maxVolts} - ${minVolts}", "trace")//2.5 -2.4 / 3 - 2.4 
           def pct = (batteryVoltage - minVolts) / (maxVolts - minVolts)

           def roundedPct = Math.round(pct * 100)
         if (roundedPct <= 0) roundedPct = 1
            batteryPercentage = Math.min(100, roundedPct)
           logging("Battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V ", "debug")   
         isChange = false
         if (powerLast != batteryPercentage){ isChange = true}
        
        logging("Battery:${batteryPercentage}%  ${batteryVoltage}V", "info")  
        sendEvent(name: "battery", value: batteryPercentage, unit: "%", isStateChange: isChange)      
        sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V", isStateChange: isChange)
           

  
    }else if (descMap.cluster == "0500"){
        
        if (descMap.attrId == "0002" ) {
        logging("0500 ${state.MFR} non iaszone.ZoneStatus report value:${descMap.value} ", "debug")    
        value = Integer.parseInt(descMap.value, 16)
        if(value == 0 ){contactClosed()}
        else if(value == 1 ){contactOpen()} 
    
        else {logging("0500 ${state.MFR} Unknown value:${value}", "debug")} // values for other brands unknown Likely same as Samjin
            
        }else if (descMap.commandInt == "07") {
          if (descMap.data[0] == "00") {
                        logging("IAS ZONE REPORTING CONFIG RESPONSE: ", "info")
                        
                       sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
                    } else {logging("IAS ZONE REPORING CONFIG FAILED - Error Code: ${descMap.data[0]} ", "warn")}
                return
                }   
if ( descMap.data){
    //   0500 command:01 options:0040 clusterInt1280 data:[03, 00, 86]  < get this a lot on all devices
    logging("0500  command:${descMap.command} options:${descMap.options} clusterInt${descMap.clusterInt} data:${descMap.data}", "debug")
 return   
}
     
    
}else if (descMap.cluster == "8034"){logging("8034 Unsubscribe received clusterInt:${descMap.clusterInt} clusterId:${clusterId.command} data:${descMap.data}", "warn")
          
// just ignore these unknown clusters for now
}else if (descMap.cluster == "0500" ||descMap.cluster == "0013" || descMap.cluster == "0006" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" || descMap.cluster == "8013") {
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
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() || status.isAlarm2Set()) {contactOpen()}
	else {contactClosed()}
}

void contactOpen(){
    test = device.currentValue("contact")
    logging("Contact: Open our state was:${test}", "debug")
    if (test != "open"){
        sendEvent(name: "contact", value: "open")
        logging("Contact: Open our state was:${test}", "info")
    }     
}

void contactClosed(){
    test = device.currentValue("contact")
    logging("Contact: Closed our state was:${test}", "debug")
    if (test != "closed"){
        sendEvent(name: "contact", value: "closed")
        logging("Contact: Closed our state was:${test}", "info")
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
   
 state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/3rd-sensor.jpg' >"
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

