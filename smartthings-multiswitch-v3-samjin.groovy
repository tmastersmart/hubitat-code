/* Smartthings multi Sensor V3 (custom)

Multipurpose Sensor V3 / Samjin (tested)
Multipurpose Sensor V2
Multipurpose Sensor V1



This driver was created to fix that bad battery reports.
It has been tested on the Samjin sensors. I dont have a smartthings version yet.

beta version is not set to enable motion reporting but will detect it if already configured.


To Reset device:
Remove battery. Insert a paper clip into the reset hole on the side of the device.
Led will turn orange Hold down then let go will flash RED GREEN 

If any problems remove device from Hubitat then Reset device. 

To go back to internal drivers without removing use uninstall then change drivers.


===================================================================================================
1.4.1    03/23/2023 Hub update broke automatic min volt test. Moved to a setting
1.3.9    01/25/2023 Power Up routine init rewriten
1.3.8    01/23/2023 Bat code event rewrite
1.3.7    12/20/2022 Debugging work
1.3.6    12/17/2022 Hex conversion bug fixed
1.3.5    12/15/2022 Min bat code rewrite. threeAxis support added
1.3.4    11/22/2022 Min voltage will adjust upward if bat dead before min setting
1.3.3    11/17/2022 cluster 0013 detection
1.3.1    11/14/2022 Updates 
1.2.0    11/12/2022 First release
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
    TheVersion="1.4.1"
if (state.version != TheVersion){
    logging("Upgrading ! ${state.version} to ${TheVersion}", "warn")
     state.version = TheVersion
     configure() 
 }
}



metadata {

definition (name: "Smartthings multi Sensor V3 (custom)", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/smartthings-multiswitch-v3-samjin.groovy") {

    capability "Health Check"
	capability "Battery"
	capability "Configuration"
	capability "Contact Sensor"
	capability "Initialize"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
	capability "TemperatureMeasurement"
    capability "Acceleration Sensor"
	capability "Three Axis"    
    
	
command "checkPresence"
command "uninstall"
 
// unable to test anything but Samjin    
attribute "batteryVoltage", "string"
attribute "threeAxis", "string"
    
    	fingerprint model:"3320",   manufacturer:"CentraLite", inClusters:"0000,0001,0003,0402,0500,0020,0B05,FC02", outClusters:"0019",     deviceJoinName: "Multipurpose Sensor"
		fingerprint model:"3321",   manufacturer:"CentraLite", inClusters:"0000,0001,0003,0402,0500,0020,0B05,FC02", outClusters:"0019",     deviceJoinName: "Multipurpose Sensor"
		fingerprint model:"3321-S", manufacturer:"CentraLite", inClusters:"0000,0001,0003,0402,0500,0020,0B05,FC02", outClusters:"0019",     deviceJoinName: "Multipurpose Sensor"
 

		fingerprint model:"multiv4",manufacturer:"SmartThings",inClusters:"0000,0001,0003,000F,0020,0402,0500,FC02", outClusters:"0019",     deviceJoinName: "Smartthings Multi Sensor v4"
		fingerprint model:"multi",  manufacturer:"Samjin",     inClusters:"0000,0001,0003,0020,0402,0500,FC02",      outClusters:"0019",     deviceJoinName: "Smartthings Multi Sensor v3"
        fingerprint model:"multi",  manufacturer:"Samjin",     inClusters:"0000,0001,0003,0020,0402,0500,FC02",      outClusters:"0003,0019",deviceJoinName: "Smartthings Multi Sensor v3",profileId:"0104", endpointId:"01"
      }
    

    
    
preferences {
		
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: true,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

   input name: "pingYes",type: "bool", title: "Enable schedule Ping", description: "", defaultValue: false,required: true
   input name: "pingIt" ,type: "enum", title: "Ping Time",description: "Ping every x mins. Press config after saving",options: ["5","10","15","20","25","30"], defaultValue: "10",required:true
   input name: "tempAdj",type: "enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8", "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8","-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: 0 ,required: true  

   input name: "minVoff",type: "enum", title: "Min Voltage",description: "Using minVoltTest set the min voltage your sensor will run on", options: ["1.9","2","2.1","2.2","2.3","2.4","2.5"], defaultValue: "2.2" ,required: true  
   input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
   input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 8 ,required: true 

    }
}

// Runs after first pairing.
def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
state.enroll =0
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
    state.remove("tempAdj"),
    state.remove("sensorTemp"),  
    state.remove("enroll"),
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
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0021)),// contact
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0022)),// contact     
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
    sendZigbeeCommands(zigbee.readAttribute(0x0500, 0x0002))// contact
}

def configure() {
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	// Sets up low battery threshold reporting
//	sendEvent(name: "DeviceWatch-Enroll", displayed: false, value: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, scheme: "TRACKED", checkInterval: 2 * 60 * 60 + 1 * 60, lowBatteryThresholds: [15, 7, 3], offlinePingable: "1"].encodeAsJSON())
//	sendEvent(name: "acceleration", value: "inactive", descriptionText: "{{ device.displayName }} was $value", displayed: false)
    logging("Configuring", "info")
    
    
    if(state.tempOffset){
        logging("Old Driver had a TempOffset of:${state.tempOffset} please manualy reselect", "warn")
        state.remove("tempOffset")
    }
    
    state.remove("sensorTemp")
    removeDataValue("threeAxis")
    removeDataValue("acceleration")

  sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
 
  state.MFR = device.getDataValue("manufacturer")  
    
    if (state.MFR == "Samjin"){ 
  delayBetween([
    refresh(),
    sendZigbeeCommands(zigbee.batteryConfig()),
    sendZigbeeCommands(zigbee.configureReporting(0x0500, 0x0021, DataType.BITMAP16, 30, 3600, null)), // min30sec max3600sec =1hr
    sendZigbeeCommands(zigbee.temperatureConfig(30, 1800)),
    sendZigbeeCommands(zigbee.enrollResponse())  

   ], 900)       
    }
    
   else { // standard
   delayBetween([
    refresh(),
    sendZigbeeCommands(zigbee.batteryConfig()),
    sendZigbeeCommands(zigbee.configureReporting(0x0500, 0x0002, DataType.BITMAP16, 30, 3600, null)), // min30sec max3600sec =1hr
    sendZigbeeCommands(zigbee.temperatureConfig(30, 1800)),
    sendZigbeeCommands(zigbee.enrollResponse())  

   ], 900)       
    }



    
  getIcons() 



    
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
	return configCmds
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
    if (!state.enroll){state.enroll =0}
    if (description?.startsWith('enroll request')) { 
     zigbee.enrollResponse()
     state.enroll = state.enroll +1   
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
    
     if (descMap.cluster == "0000" ) {//0x0000 Basic Attributes  
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
        def  maxVolts  = 2.9
    


    if (minVoff == "1.9"){minVolts = 1.9 }
    if (minVoff == "2.0"){minVolts = 2 }
    if (minVoff == "2.1"){minVolts = 2.1 }
    if (minVoff == "2.2"){minVolts = 2.2 }
    if (minVoff == "2.3"){minVolts = 2.3 }
    if (minVoff == "2.4"){minVolts = 2.4 }
    if (minVoff == "2.5"){minVolts = 2.5 }
    
       if (state.MFR == "SmartThings") {
			minVolts = 15 // this is defaut from st driver untested
			maxVolts = 28
          }        
    
// hub update broke this.   in testing 2.4 is min on some sensors. 2.1 is the min on others 
//        if(state.minVoltTest){minVolts = state.minVoltTest} // this should hold the lowest voltage if set
    
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
    
        
    }  else if (descMap.cluster == "0402" ) {//0x0402 Temperature measurement Attributes
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
        

        


    }else if (descMap.cluster == "FC02"){// these are motion events.
// [raw:E44601FC020810001800, dni:E446, endpoint:01, cluster:FC02, size:08, attrId:0010, encoding:18, command:0A, value:00, clusterInt:64514, attrInt:16]    
        logging("FC02 Motion Event. command:${descMap.command} value:${descMap.value} ${descMap.additionalAttrs} ${descMap.attrInt}", "debug")
        if (descMap.value == "01"){
           logging("acceleration active", "info")
           sendEvent(name: "acceleration", value: "active")     
        }
        if (descMap.value == "00"){
           logging("acceleration inactive", "info")
           sendEvent(name: "acceleration", value: "inactive" )
        }

        if (descMap.attrInt == 0x0010){ parseAxis(descMap.additionalAttrs)}    
        if (descMap.attrInt == 0x0012){
        def addAttrs = descMap.additionalAttrs ?: []
		addAttrs << ["attrInt": descMap.attrInt, "value": descMap.value]
		parseAxis(addAttrs)
        }    
            
    return
       
        

}else if (descMap.cluster == "0500"){//0x0500 IAS Zone Attributes and commands for IAS security zone devices.

        if (descMap.attrId == "0002" ) {
         value = Integer.parseInt(descMap.value, 16)
            if(value == 32 ){
                logging("${state.MFR} Contact event cluster:500 value:${value} CLOSED", "debug")
                contactClosed()
                return
            }
            else if(value == 33 ){
                logging("${state.MFR} Contact event cluster:500 value:${value} OPEN", "debug")
                contactOpen()
                return
            }
           // 3320-L 37/36  iMagic 1/0  Samjin 32/33
           else {logging("ERROR: ignoring event cluster:500 not a 1/0 Contact event. Unknown value:${value}", "debug")}
            
      } else if (descMap.commandInt == "07") {
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
}else if (descMap.cluster == "0500" ||descMap.cluster == "0006" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" ) {
   text= ""
      if (descMap.cluster =="8001"){text="GENERAL"}
 else if (descMap.cluster =="8021"){text="BIND RESPONSE"}
 else if (descMap.cluster =="8031"){text="Link Quality"}
 else if (descMap.cluster =="8032"){text="Routing Table"}
   
   if (descMap.data){text ="${text} clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Ignoring ${descMap.cluster} ${text}", "debug") 
        
}else if (descMap.cluster =="0013"){
  logging("${descMap.cluster} Unknown multistate event - data:${descMap.data}", "debug") // Low Bat? unknown
        

 }  else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

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

// sends standard zigbee command
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() || status.isAlarm2Set()) {
        contactOpen()
    }else {contactClosed()}
}

private List<Map> parseAxis(List<Map> attrData) {
	def results = []
	def x = hexToSignedInt(attrData.find { it.attrInt == 0x0012 }?.value)
	def y = hexToSignedInt(attrData.find { it.attrInt == 0x0013 }?.value)
	def z = hexToSignedInt(attrData.find { it.attrInt == 0x0014 }?.value)
	if ([x, y ,z].any { it == null }) {	return []}
    
    logging("parseAxis -- ${attrData} x${x} y${y} z${z}","trace")
	def xyzResults = [:]
	if (device.getDataValue("manufacturer") == "SmartThings") {
		// This mapping matches the current behavior of the Device Handler for the Centralite sensors
		xyzResults.x = z
		xyzResults.y = y
		xyzResults.z = -x
	} else {
		// The axises reported by the Device Handler differ from the axises reported by the sensor
		// This may change in the future
		xyzResults.x = z
		xyzResults.y = x
		xyzResults.z = y
	}


//	if (garageSensor == "Yes")
//		results += garageEvent(xyzResults.z)

	def value = "${xyzResults.x},${xyzResults.y},${xyzResults.z}"
    logging("xyz ${value} Sensor:xyz${x}-${y}-${z}","debug")
    logging("threeAxis position received ","info")
    sendEvent(name: "threeAxis", value: value, descriptionText: "xyz ${value} Sensor:xyz${x}-${y}-${z}")
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
         batteryVoltage = device.currentValue("batteryVoltage")   
         state.minVoltTest = batteryVoltage
         logging("Min Voltage reset to ${state.minVoltTest}v", "info")
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

private hexToSignedInt(hexVal) {
    if (!hexVal) {return null}
    def unsignedVal = HexUtils.hexStringToInt(hexVal)
//  def unsignedVal = convertHexToInt(hexVal)
//	def unsignedVal = hexToInt(hexVal)
	unsignedVal > 32767 ? unsignedVal - 65536 : unsignedVal
    logging("Hex ${hexVal} to signed Int ${unsignedVal}","trace")
    return unsignedVal
}
void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

void getIcons(){
if (state.MFR == "Samjin"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/Samjin.jpg' >"}

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

