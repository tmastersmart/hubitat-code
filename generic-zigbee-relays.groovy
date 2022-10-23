/** Zigbee Sonoff - generic relays
driver for hubitat

Sonoff MINI ZB  relay driver
Generic zigbee relays/outlets...


This driver was created to handel my SONOFF and off brand relays.
Send me your fngerprints so they can be added.




v 2.0.0 10/13/2022   Creation
==============================================================================================
GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
 *	
 */
def clientVersion() {
    TheVersion="1.0.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils
metadata {
    
	definition (name: "Zigbee Sonoff - generic relays", namespace: "tmastersmart", author: "tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/generic-zigbee-relays.groovy") {

		capability "Actuator"
		capability "Configuration"
		capability "EnergyMeter"
		capability "Initialize"
		capability "Outlet"
		capability "PresenceSensor"
		capability "Refresh"
		capability "Switch" 
        capability "Alarm"



        command "unschedule" 
        command "uninstall"


		attribute "strobe", "string"
		attribute "siren", "string"


        fingerprint model:"BASICZBR3",manufacturer:"SONOFF",deviceJoinName:"SONOFF Relay BASICBR3",profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006",outClusters:"0000"
	    fingerprint model:"01MINIZB", manufacturer:"SONOFF",deviceJoinName:"SONOFF Relay MINI",    profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,FC57",outClusters:"0019"	
	}

}

preferences {
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
	
}


def installed() {
	// Runs after first pairing. this may never run internal drivers overide pairing.
	logging("${device} : Paired!", "info")
}

def uninstall() {
	unschedule()
	state.remove("presenceUpdated")    
	state.remove("version")
    logging("Uninstalled", "info")   
}

def initialize() {

    // This runs on reboot 
	state.presenceUpdated = 0
    state.logo =""

	// Remove disused state variables from earlier versions.
state.remove("status")

	// Remove unnecessary device details.
    device.deleteCurrentState("alarm")

    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
}


def configure() {
	// Runs on reboot paired or rejoined
    logging("configure", "info")  
    state.DataUpdate = false  
	unschedule()
//    configureDevice()
    

	
    // Schedule presence in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${12} * * ? *", checkPresence)	
   
refresh() 
}

void configureDevice() {
    Integer endpointId = 1
    ArrayList<String> cmd = []
    cmd += zigbee.readAttribute(0x0000, [0x0001, 0x0004, 0x0005, 0x0006])
    cmd += ["zdo bind 0x${device.deviceNetworkId} ${endpointId} 0x01 0x0001 {${device.zigbeeId}} {}", "delay 187"]
    cmd += zigbee.readAttribute(0x0001, [0x0020, 0x0021])
    cmd += ["he cr 0x${device.deviceNetworkId} ${endpointId} 0x0001 0 0x10 0 0xE10 {}", "delay 189"]

    sendZigbeeCommands(cmd)
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
}

void reportToDev(map) {
	String[] receivedData = map.data
	logging("New unknown Cluster Detected: clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command}, value:${map.value} data: ${receivedData}", "warn")
}




void refresh(cmd) {
	logging("Refreshing", "info")
    Test = device.currentValue("switch")

    if (Test =="on"){
        logging("Resending State ON", "info")
        runIn(2,on)
    }
    else {
        logging("Resending State OFF", "info")
        runIn(2,off)
    }    
    
       delayBetween([
            zigbee.readAttribute(0x0000, 0x0006), 
            zigbee.readAttribute(0x0000, 0x0004),
 //           sendZigbeeCommands(["zcl global send-me-a-report 6 0 0x10 0 1 {01}"]),
 //           sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x0006 0x0001"]),
   ], 1000) 
    
}






def alarm(cmd){
    logging("Alarm ON", "info")
    sendEvent(name: "alarm", value: "on")
  on()
}
                   
def siren(cmd){
    logging("siren ON", "info")
    sendEvent(name: "siren", value: "on")
  on()
}
def strobe(cmd){
    logging("strobe ON", "info")
    sendEvent(name: "strobe", value: "on")
  on()
}
def both(cmd){
    logging("both ON", "info")
    sendEvent(name: "siren", value: "on")
    sendEvent(name: "strobe", value: "on")
  on()
}

def off() {
    logging("Sending OFF", "debug")
	zigbee.command(0x006, 0x00)
}

def on() {
    logging("Sending ON", "debug")
	zigbee.command(0x006, 0x01)
}


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


def checkPresence() {
    // New shorter presence routine.
    // Runs on every parse and a schedule.
    def checkMin  = 5  // 5 min warning
    def checkMin2 = 10 // 10 min [not present] and 0 batt
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins","debug")
    if (state.lastCheckInMin <= checkMin){ 
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
        logging("Creating presence event: ${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin){ 
        logging("Sensor timing out ${state.lastCheckInMin} min ago","warn")
        runIn(60,refresh)// Ping Perhaps we can wake it up...
    }
    if (state.lastCheckInMin >= checkMin2) { 
        test = device.currentValue("presence")
        if (test != "not present"){
        value = "not present"
        logging("Creating presence event: ${value} ${state.lastCheckInMin} min ago","warn")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        runIn(60,refresh) 
        }
    }
}

def enrollResponse() {
    logging("Sending enroll response", "info")
	String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
   
    delayBetween([
            sendZigbeeCommands(["zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}"]),// send CIE
            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 ${endpointId}"]),
            sendZigbeeCommands(["raw 0x500 {01 23 00 00 00}"]),// enrole res
            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 1"]),	
   ], 1000)
    
}

def parse(String description) {
    state.lastCheckin = now()
    checkPresence()
    logging("Raw: [${description}]", "trace")
    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("MAP: [${descMap}]", "trace")

    if (description?.startsWith('enroll request')) { 
        enrollResponse()
        return  
    }  
    


    if (descMap) {processMap(descMap)}
        else{
        // we should never get here reportToDev is in processMap above
            logging("Error ${description} ${descMap}", "debug") 

        }
	
}






def processMap(Map map) {
	String[] receivedData = map.data
  
    logging("PARSE [${map}]", "trace")
//MAP: [[raw:BD840100060800002001, dni:BD84, endpoint:01, cluster:0006, size:08, attrId:0000, encoding:20, command:0A, value:01, clusterInt:6, attrInt:0]]  
//MAP: [[raw:catchall: 0104 0006 01 01 0040 00 BD84 00 00 0000 0B 01 0100, profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01,
    //options:0040, messageType:00, dni:BD84, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[01, 00]]]
    
    if (map.clusterId == "0006" && map.profileId == "0104"  ){
      logging("clusterId:${map.clusterId} profileId:${map.profileId} command:${map.command}", "debug")
      status = map.data[0]
        if (status == "01"){
        logging("is ON [digital]", "info")
        sendEvent(name: "switch", value: "on")
        }
        if (status == "00"){
        logging("is OFF [digital]", "info")
        offEvents() 
        
        }

    } 
        
        
 
//New unknown Cluster Detected: clusterId:null, attrId:0000, command:0A, value:00 data: null
//PARSE [[raw:BD840100060800002000, dni:BD84, endpoint:01, cluster:0006, size:08, attrId:0000, encoding:20, command:0A, value:00, clusterInt:6, attrInt:0]]	
        
   
    else if (map.cluster == "0006") {
        logging("cluster:${map.cluster} command:${map.command} value:${map.value}", "debug")
        sendEvent(name: "pushed", value: 1, isStateChange: true)
        status = map.value
        if (status == "01"){
        logging("Button Pressed is ON [physical] ", "info")
        sendEvent(name: "switch", value: "on")
        }
        if (status == "00"){
        logging("Button Pressed is OFF [physical] ", "info")
        offEvents() 
        
        }
        
        
	} else {
		reportToDev(map)// unknown cluster
	}
	return null
}

def offEvents(){
    alarmTest = device.currentValue("alarm")   
    if(alarmTest != "off"){sendEvent(name: "alarm",  value: "off")}
    alarmTest = device.currentValue("siren") 
    if(alarmTest != "off"){sendEvent(name: "siren",  value: "off")} 
    alarmTest = device.currentValue("strobe") 
    if(alarmTest != "off"){sendEvent(name: "strobe", value: "off")}
    alarmTest = device.currentValue("switch")
    if(alarmTest != "off"){sendEvent(name: "switch", value: "off")} 
}
    
    


void sendZigbeeCommands(List<String> cmds) {
    logging("${device} : sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}


private String[] millisToDhms(BigInteger millisToParse) {
	BigInteger secondsToParse = millisToParse / 1000
	def dhms = []
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 24)
	secondsToParse = secondsToParse / 24
	dhms.add(secondsToParse % 365)
	return dhms
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



private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}

// Logging block v4 10/23/2022
//	
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    if (debugLogging){runIn(3600,debugLogOff)}
    if (traceLogging){runIn(1800,traceLogOff)}
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
