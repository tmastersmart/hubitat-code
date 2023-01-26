/** eWeLight RGBW Bulbs - Zigbee ZB-CL01
driver for hubitat Seedan eWeLight ZB-CL01 RGBW Bulbs and others



This driver was created to handel 
ZB-CL01 - eWeLight RGBW bulbs

May work with generics


Seedan Zigbee Smart Light Bulbs, Color Changing Light Bulb, 9W 806LM, Smart Bulb Compatible with Amazon
Alexa and Samsung SmartThings Hub, Hub Required, E26 Dimmable LED Bulb, 2 Pack
======================================================================================================
v1.0.3  01/26/2023   Fixed Color Temp Bug
v1.0.2  01/25/2023   Power up routine rewrite
v1.0.1  01/03/2023   Release
v1.0.0  12/30/2022   Creation
======================================================================================================
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
=======================================================================================================


colorMode RGB / CT

 *	
 */
def clientVersion() {
    TheVersion="1.0.3"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils
metadata {
    
definition (name: "eWeLight/Seedan RGBW Bulbs - Zigbee", namespace: "tmastersmart", author: "tmaster", importUrl: "") {

capability "Health Check"
capability "Actuator"
capability "Configuration"
capability "Initialize"
capability "PresenceSensor"
capability "Refresh"
capability "Switch"
capability "Switch Level"
capability "Color Control"
capability "Color Temperature"
capability "Light"

command "uninstall"
command "checkPresence"

attribute "colorName", "string"
attribute "colorMode", "string"
        
fingerprint profileId:"C05E", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,0300", model:"ZB-CL01", manufacturer:"eWeLight"// RGBW Bulb RGB and CT (Seedan)
 }
}
// If the above fingerprint doesnt work please send me yours. You can get it using internal GENERIC DEVICE driver Just press info



preferences {
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true


    input name: "autoSync",     type: "bool", title: "AutoSync to hub state", description: "Recovery from powerfalure, Keeps bulb in sync with digital state. Do not use with group Messaging it is unable to see the group commans and will malfunction!", defaultValue: true,required: true
    input name: "resendState",  type: "bool", title: "Resend Last State on Refresh", description: "For problem devices that dont wake up or dont reply to commands.", defaultValue: false,required: true
    input name: "pollHR" ,	    type: "enum", title: "Check Presence Hours",description: "Chron Schedule. 0=disable Press config after saving",options: ["0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: "10",required: true 

	input name: "disableLogsOff",type: "bool", title: "Disable Auto Logs off", description: "For debugging doesnt auto disable", defaultValue: false,required: true
   
}

// Runs after first pairing.
def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
pollHR = 10
pingIt = 30 
state.minVoltTest = 2.2   
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






def uninstall() {
	unschedule()
	state.remove("presenceUpdated")    
	state.remove("version")
    state.remove("checkPhase")
    state.remove("lastCheckInMin")
    state.remove("logo")
    state.remove("bin")
    state.remove("DataUpdate")
    state.remove("lastCheckin")
    state.remove("lastPoll")
    state.remove("donate")
    state.remove("model")
    state.remove("MFD")
    state.remove("tries")

    logging("Uninstalled", "info")  
}




def configure() {
    logging("Config", "info") 
	// Runs on reboot paired or rejoined
    
   // clean up trash from internal driver 
   state.remove("tt")
   state.remove("ct") 
   state.remove("groups") 
   state.remove("xyOnly")
   state.remove("hexLevel")
   state.remove("lastAddress")
   state.remove("checkPhase")
    
   // Remove old unused states from beta
   state.remove("icon")
   state.remove("logo") 
   state.remove("lastCT")
   state.remove("ct") 
   state.remove("hueH") 
   state.remove("level")
   state.remove("lastSaturation")
   state.remove("lastHue")
   state.remove("hue")
   state.remove("Alarm")
   state.remove("satH")
    
	unschedule()
    
    if (!pollHR){ pollHR= 8}
    state.poll = pollHR
    
    if (state.poll != 0){ 
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)    
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
    logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${state.poll} * * ? *", "debug") 
    schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${state.poll} * * ? *", checkPresence)	
    logging("Presence Check Every ${state.poll}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
    }     
    
    getIcons()
    
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    ping() 
    getIcons()
}



void refresh(cmd) {
    if(state.DataUpdate){ logging("Refreshing ${state.MFR} ${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}
delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
//    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000)),// switch
    sendZigbeeCommands(zigbee.onOffRefresh()),
    sendZigbeeCommands(zigbee.levelRefresh()), 
    sendZigbeeCommands(zigbee.colorTemperatureRefresh()),
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0000)),//COLOR_CONTROL_CLUSTER - hue
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0001)),//COLOR_CONTROL_CLUSTER - saturation
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0007)),// color - read color temp
   ], 1500)    
  
    if(resendState){sync()}
}

def ping() {
    logging("Ping ", "info")
    if(resendState){sync()}
delayBetween([    
    sendZigbeeCommands(zigbee.onOffRefresh()),
    sendZigbeeCommands(zigbee.levelRefresh()), 
//    sendZigbeeCommands(zigbee.colorTemperatureRefresh()),
//    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))// switch
       ], 1500)   
}

// Resend the proper state to get back in sync
def sync (){
    if(autoSync == false){return}
    if(!state.error){state.error = 0}
    state.error = state.error +1 
    if(state.error > 12){
    logging("Loss of control. Resync falure. Errors:${state.error}", "warn")
    runIn(10,ping)    
    return // prevent a non stop loop  
    }
    
    logging("Resyncing State. Errors:${state.error}", "warn") 
    if (state.switch== true){ runIn(4,on)}
    else {runIn(4,off)}
}



def off() {
    state.switch = false
    runIn(20,ping)
    logging("Sending OFF ${state.switch}", "info")
	sendZigbeeCommands(zigbee.command(0x006, 0x00))
}

def on() {
    state.switch = true
    runIn(20,ping)
    logging("Sending ON ${state.switch}", "info")
	sendZigbeeCommands(zigbee.command(0x006, 0x01))
}



def checkPresence() {
    // presence routine. v5 11-12-22
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 200
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


def parse(String description) {
    logging("Parse: [${description}]", "trace")
    state.lastCheckin = now()
    checkPresence()
   
    if (description?.startsWith('enroll request')) { 
     zigbee.enrollResponse()
     return  
    }  
    
//   	if (description.startsWith("zone status")) {// iaszone.ZoneStatus
//		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
//		processStatus(zoneStatus)
//        return
//    }

    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("MAP: ${descMap}", "debug")    

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
        getIcons() // Get icon for this model.  
        return    
        } 
 }   
 	def evt = zigbee.getEvent(description)
    if (evt){logging("Event: ${evt}", "debug")} 
    
// New event detction without using clusters 
    if (evt.name == "saturation"){
        logging("saturation: ${evt.value} #${descMap.value}", "info")    
		sendEvent(name: "saturation", value: evt.value, displayed:true)
        return

    } else if (evt.name == "hue"){
       hue = evt.value
       getColor(hue)    
       logging("Color:${state.color} hue:${hue}", "info")
       logging("hue:${hue} #${descMap.value}", "debug")   
	   sendEvent(name: "hue", value: hue, displayed:true)
       sendEvent(name: "colorName", value: state.color, displayed:true, descriptionText:"Color name ${state.color} ${evt.value}°K ${state.version}", isStateChange: true)  
       sendEvent(name: "colorMode", value: "RGB", displayed:true )  
       return

    } else if (evt.name == "level"){
       status = descMap.value
       logging("level: ${evt.value}  #${status}", "info")    
	   sendEvent(name: "level", value: evt.value, displayed:true)
       return        
        
    } else if (evt.name == "switch"){ 
       if (evt.value == "on") {onEvents()}
       if (evt.value == "off"){offEvents()} 
       return  
        
    } else if (evt.name == "colorTemperature") {
       getColorT(evt.value)
       logging("color:${state.color} ${evt.value}°K", "info")
       logging("color:${evt.value}°K #${descMap.value}", "debug")  
       sendEvent(name: "colorName", value: state.color, displayed:true, descriptionText:"Color name ${state.color} ${evt.value}°K ${state.version}", isStateChange: true)  
       sendEvent(name: "colorTemperature", value: evt.value, displayed:true, unit: "°K",descriptionText:"ColorTemp ${evt.value}°K ${state.version}", isStateChange: true) 
       sendEvent(name: "colorMode", value: "CT", displayed:true ) 
       return  
} 
// end events start cluster detection   
    
// just ignore these unknown clusters for now
if (descMap.cluster == "0500" ||descMap.cluster == "0008" ||descMap.cluster == "0300" ||descMap.cluster == "0006"  || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" || descMap.cluster == "8001" ) {
   text= ""
      if (descMap.cluster =="8001"){text="GENERAL"}
 else if (descMap.cluster =="8021"){text="BIND RESPONSE"}
 else if (descMap.cluster =="8031"){text="Link Quality"}
 else if (descMap.cluster =="8032"){text="Routing Table"}

   
   if (descMap.data){text ="${text} clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Ignoring ${descMap.cluster} ${text}", "debug") 

}else if (descMap.cluster =="0013"){logging("${descMap.cluster} Multistate event (Rejoining) data:${descMap.data}", "debug") 
   
        
}else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

}


// prevent dupe events
def onEvents(){
    Test = device.currentValue("switch")
    logging("is ON our state was:${Test}", "info")
    if (Test != "on"){sendEvent(name: "switch", value: "on",isStateChange: true)}
    if (autoSync== true){ 
    if (state.switch == false){sync()}
    else{state.error =0}   
    }
}
def offEvents(){
    Test = device.currentValue("switch")
    if (Test != "off"){ sendEvent(name: "switch", value: "off",isStateChange: true)}
    logging("is OFF our state was:${Test}", "info")
    if (autoSync== true){ 
    if (state.switch == true){sync()}
    else{state.error =0}    
    }
}

//0x0000 ATTRIBUTE_HUE
//0x0001 ATTRIBUTE_SATURATION
//0x00   HUE_COMMAND
//0x03   SATURATION_COMMAND
//0x06   MOVE_TO_HUE_AND_SATURATION_COMMAND
//0x0300 COLOR_CONTROL_CLUSTER
//0x0007 ATTRIBUTE_COLOR_TEMPERATURE
//0x0A   MOVE_TO_COLOR_TEMPERATURE_COMMAND

private getColorT(value){
       def colorName = "NA"// color grid
       if (     value <= 2000){colorName = "Sodium"      }// bulbs bottom out at 2000
       else if (value <= 2100){colorName = "Starlight"   }
       else if (value < 2400) {colorName = "Sunrise"     }
       else if (value < 2800) {colorName = "Incandescent"}
       else if (value < 3300) {colorName = "Soft White"  }
       else if (value < 3500) {colorName = "Warm White"  }
       else if (value < 4150) {colorName = "Moonlight"   }
       else if (value <= 5000){colorName = "Horizon"     }
       else if (value < 5500) {colorName = "Daylight"    }
       else if (value < 6000) {colorName = "Electronic"  }
       else if (value <= 6500){colorName = "Skylight"    }
       else if (value <= 20000){colorName = "Polar"      }// bulbs top out at 10000
       state.color = colorName 
}

private getColor(hue){
def colorName = "NA"  
    hue = hue.toInteger()
    hue = (hue * 3.6)    
    switch (hue.toInteger()){
    case 0..15: colorName = "Red"
    break
    case 16..45: colorName = "Orange"
    break
    case 46..75: colorName = "Yellow"
    break
    case 76..105: colorName = "Chartreuse"
    break
    case 106..135: colorName = "Green"
    break
    case 136..165: colorName = "Spring"
    break
    case 166..195: colorName = "Cyan"
    break
    case 196..225: colorName = "Azure"
    break
    case 226..255: colorName = "Blue"
    break
    case 256..285: colorName = "Violet"
    break
    case 286..315: colorName = "Magenta"
    break
    case 316..345: colorName = "Rose"
    break
    case 346..360: colorName = "Red"
    break
   }
   state.color = colorName
}
def setColorTemperature(value){
setColorTemperature(value,100,0)
}

// CT routines
def setColorTemperature(value,lvl,nu) {
	value = value as Integer
    getColorT(value)
	def tempInMired = Math.round(1000000 / value)
    def tempHex = integerToHexString(tempInMired, 4)
	def finalHex = swapEndianHex(tempHex)
    logging("Send Color:${state.color} ${value}°K", "info")
    logging("Send Color hex${tempHex} swapEndianHex${finalHex}", "devug")
    runIn(20,ping)
    delayBetween([
    sendZigbeeCommands(zigbee.command(0x0300, 0x0A, "$finalHex 0000")),// color - move to color temp
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0007)),// color - read color temp
//   sendZigbeeCommands(zigbee.colorTemperatureRefresh()),
   ], 1500) 
    
}


def setLevel(value, rate = null) {
    logging("Send SetLevel ${value}", "info")
    runIn(20,ping)
    delayBetween([
    sendZigbeeCommands(zigbee.setLevel(value)),
    sendZigbeeCommands(zigbee.levelRefresh()),
   ], 1500)     

}


def setColor(value){
//     def rate = value?.rate ? value.rate * 400 : (transitionTime?.toInteger() ?: 400)
//     rateH = intTo16bitUnsignedHex(rate / 100)
     def sat3 = zigbee.convertToHexString(Math.round(value.saturation * 254 / 100).toInteger(),2)
     def hue3 = zigbee.convertToHexString(Math.round(value.hue * 254 / 100).toInteger(),2)
//     def level = (value.level.toInteger() * 2.55).toInteger()
//     lvlHex = intTo8bitUnsignedHex(level)
    getColor(value.hue)
    logging("Send SetColor ${state.color} ${value}", "info")
	logging("Send SetColor sat#${sat3} hue#${hue3} level${value.level}", "debug")
//sendZigbeeCommands(zigbee.command(0x006, 0x01)),// send on
//    sendZigbeeCommands(zigbee.command(0x0008,0x04,lvlHex,rateH)    )    
//    sendZigbeeCommands(zigbee.command(0x0300 , 0x00, hue, "00", "0000")),// set hue
//    sendZigbeeCommands(zigbee.command(0x0300 , 0x01, sat, "00", "0000")),// set saturation    
    
    delayBetween([
    sendZigbeeCommands(zigbee.command(0x0300,0x06,hue3,sat3, "0000")),  // MOVE_TO_HUE_AND_SATURATION_COMMAND
    sendZigbeeCommands(zigbee.setLevel(value.level)),     
    sendZigbeeCommands(zigbee.onOffRefresh()),
    sendZigbeeCommands(zigbee.levelRefresh()), 
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0000)), // read hue
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0001)), // read saturation       
   ], 1000)     
    
    runIn(2,on)// on cmd not needed  for compatability with some firmwares
}


// This is what alexa and google use to set color to red or green
def setHue(value) {
    sat = device.currentValue("saturation")
    def hueH = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    def satH = zigbee.convertToHexString(Math.round(sat * 254 / 100).toInteger(),2) 
    runIn(20,ping)
    logging("Send setHue ${value} hue#${hueH} sat#${satH}", "info")
    delayBetween([
    sendZigbeeCommands(zigbee.command(0x0300,0x00,hueH, "00", "0000")),
    sendZigbeeCommands(zigbee.command(0x0300,0x06,hueH,satH, "0000")),  // MOVE_TO_HUE_AND_SATURATION_COMMAND    
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0000)), // read hue
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0001)), // read saturation 
    ], 1500) 
    runIn(2,on)// on cmd not needed  for compatability with some firmwares
}

def setSaturation(value) {
    hue = device.currentValue("hue")
    def hueH = zigbee.convertToHexString(Math.round(hue * 254 / 100).toInteger(),2)
    def satH = zigbee.convertToHexString(Math.round(value * 254 / 100).toInteger(),2)
    runIn(20,ping)
    logging("Send setSaturation ${value}  hue#${hueH} sat#${satH}", "info")
    delayBetween([
//  sendZigbeeCommands(zigbee.command(0x0300 , 0x01, sat3, "00", "0000")),
    sendZigbeeCommands(zigbee.command(0x0300,0x06,hueH,satH, "0000")),  // MOVE_TO_HUE_AND_SATURATION_COMMAND       
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0000)), // read hue
    sendZigbeeCommands(zigbee.readAttribute(0x0300, 0x0001)), // read saturation 
    ], 1500) 
}



void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}


// Hex helping routines

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}
private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}


String integerToHexString(BigDecimal value, Integer minBytes, boolean reverse=false) {
    return integerToHexString(value.intValue(), minBytes, reverse=reverse)
}

String integerToHexString(Integer value, Integer minBytes, boolean reverse=false) {
    if(reverse == true) {
        return HexUtils.integerToHexString(value, minBytes).split("(?<=\\G..)").reverse().join()
    } else {
        return HexUtils.integerToHexString(value, minBytes)
    }
    
}





void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    
    if (state.model == "ZB-CL01"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/zb-cl01.jpg' >"}
 

 }

// Logging block v5 11/2022
//	
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    if (disableLogsOff){return}
    if (debugLogging){runIn(4000,debugLogOff)}
    if (traceLogging){runIn(1000,traceLogOff)}
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
