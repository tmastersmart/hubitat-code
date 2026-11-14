/* Sonoff SNZB-02DR2 Temperature Humidity Sensor for Hubitat
A Sonoff SNZB-02DR2 (AirGuard TH) Driver For Hubitat
manufacturer: SONOFF / eWeLink
model: SNZB-02DR2
===================================================================================================
v1.0.0  11/14/2026 First release - Temp / Humidity / Battery + LCD unit control
=================================================================================================== 
Copyright [2026] [tmaster winnfreenet.com]
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
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

def clientVersion() {
    TheVersion = "1.0.0"
    if (state.version != TheVersion) {
        logging("Upgrading ! ${state.version} to ${TheVersion}", "warn")
        state.version = TheVersion
        configure()
    }
}

metadata {
    definition (name: "Sonoff SNZB-02DR2 Temp Humidity", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/refs/heads/main/sonoff_SNZB-02DR2.groovy") {
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "Battery"
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"
        capability "Health Check"
        capability "PresenceSensor"

        command "checkPresence"
        command "uninstall"
        command "setLcdUnitC"
        command "setLcdUnitF"
        
        attribute "temperatureMin", "number"
        attribute "temperatureMax", "number"
        attribute "humidityMin", "number"
        attribute "humidityMax", "number"
        
        attribute "batteryVoltage", "string"
        attribute "lastUpdate", "string"

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0001,0003,0402,0405,0020,FC57,FC11", outClusters: "000A,0019",  model: "SNZB-02DR2", manufacturer: "SONOFF", deviceJoinName: "Sonoff SNZB-02DR2 AirGuard TH"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0001,0003,0402,0405,0020,FC57,FC11", outClusters: "000A,0019",  model: "SNZB-02DR2", manufacturer: "eWeLink",deviceJoinName: "Sonoff SNZB-02DR2 AirGuard TH"
    }

    preferences {
        input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recommended low level", defaultValue: true, required: true
        input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug", defaultValue: true, required: true
        input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false, required: true
        input name: "tempUnit", type: "enum", title: "Temperature Unit (Hub display)", description: "How temperature is shown in Hubitat. Device LCD is controlled separately below.", options: ["C": "Celsius (°C)", "F": "Fahrenheit (°F)"],defaultValue: "F", required: true
        input name: "tempOffset", type: "decimal", title: "Temperature Offset", description: "Adjust reported temperature (in the unit selected above)", range: "-10..10", defaultValue: 0.0
        input name: "humOffset", type: "decimal", title: "Humidity Offset (%)", description: "Adjust reported humidity",  range: "-20..20", defaultValue: 0.0
        input name: "pollYes", type: "bool", title: "Enable Presence", description: "", defaultValue: true, required: true
        input name: "pollHR", type: "enum", title: "Check Presence Hours",description: "Press config after saving", options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: "8", required: true
    }
}

def installed() {
    logging("Installed", "warn")
    state.DataUpdate = false
    pollHR = 8
    configure()
    updated()
    initialize()
}

def initialize() {
    logging("initialize", "debug")
    clientVersion()
    randomSixty = Math.abs(new Random().nextInt() % 180)
    runIn(randomSixty, refresh)
}

def uninstall() {
    delayBetween([
        unschedule(),
        state.remove("version"),
        state.remove("presenceUpdated"),
        state.remove("lastCheckInMin"),
        state.remove("DataUpdate"),
        state.remove("lastCheckin"),
        state.remove("lastPoll"),
        state.remove("model"),
        state.remove("MFR"),
        state.remove("minVoltTest"),
        logging("Uninstalled - States removed you may now switch drivers", "info"),
    ], 200)
}

def updated() {
    logging("Updated", "info")
    loggingUpdate()
    clientVersion()
}

def refresh() {
    if (state.MFR) {
        logging("Refreshing ${state.MFR} Model:${state.model} Ver:${state.version}", "info")
    } else {
        logging("Refreshing -unknown device-  Ver:${state.version}  (press button on sensor then click Refresh)", "info")
    }

    delayBetween([
        sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)), // manufacturer
        sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)), // model
        sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)), // battery voltage
        sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0021)), // battery %
        sendZigbeeCommands(zigbee.readAttribute(0x0402, 0x0000)), // temperature
        sendZigbeeCommands(zigbee.readAttribute(0x0405, 0x0000)), // humidity

        // 24h min/max (must wake device first)
        sendZigbeeCommands(zigbee.readAttribute(0xFC11, 0x2008, [mfgCode: 0x1286])), // Temp Max
        sendZigbeeCommands(zigbee.readAttribute(0xFC11, 0x2009, [mfgCode: 0x1286])), // Temp Min
        sendZigbeeCommands(zigbee.readAttribute(0xFC11, 0x200B, [mfgCode: 0x1286])), // Hum Max
        sendZigbeeCommands(zigbee.readAttribute(0xFC11, 0x200C, [mfgCode: 0x1286])), // Hum Min
    ], 600)
}

def ping() {
    logging("Ping", "info")
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0021)) // battery %
}

def configure() {
    logging("Config", "info")
    getIcons()

    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false,
        data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

    delayBetween([
        refresh(),
        sendZigbeeCommands(zigbee.batteryConfig()),
        // Temperature reporting: min 30s, max 30min, change 0.2°C
        sendZigbeeCommands(zigbee.configureReporting(0x0402, 0x0000, DataType.INT16, 30, 1800, 20)),
        // Humidity reporting: min 30s, max 30min, change 1%
        sendZigbeeCommands(zigbee.configureReporting(0x0405, 0x0000, DataType.UINT16, 30, 1800, 100)),
    ], 900)

    unschedule()

    if (pollYes) {
        randomSixty = Math.abs(new Random().nextInt() % 60)
        randomSixty2 = Math.abs(new Random().nextInt() % 60)
        randomTwentyFour = Math.abs(new Random().nextInt() % 24)
        logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", "debug")
        schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", checkPresence)
        logging("Presence Check Every ${pollHR}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2}", "info")
    }
}

// ========== Parse ==========
def parse(String description) {
    logging("Parse: [${description}]", "trace")
    state.lastCheckin = now()
    checkPresence()

    Map descMap = zigbee.parseDescriptionAsMap(description)
    logging("MAP: ${descMap}", "trace")

    if (descMap.clusterId) { descMap.cluster = descMap.clusterId }

    // Basic cluster - Manufacturer / Model
    if (descMap.cluster == "0000") {
        if (descMap.attrId == "0004" && descMap.attrInt == 4) {
            logging("Manufacturer :${descMap.value}", "debug")
            state.MFR = descMap.value
            updateDataValue("manufacturer", state.MFR)
            state.DataUpdate = true
            return
        }
        if (descMap.attrId == "0005" && descMap.attrInt == 5) {
            logging("Model :${descMap.value}", "debug")
            state.model = descMap.value
            updateDataValue("model", state.model)
            state.DataUpdate = true
            return
        }
    }

    // Power Configuration - Battery
    if (descMap.cluster == "0001") {
        // Battery Voltage (0x0020)
        if (descMap.attrId == "0020") {
            def rawValue = Integer.parseInt(descMap.value, 16)
            if (rawValue == 0 || rawValue == 255) return
            def batteryVoltage = rawValue / 10.0
            logging("Battery Voltage: ${batteryVoltage}V", "info")
            sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
            return
        }
        // Battery Percentage (0x0021)
        if (descMap.attrId == "0021") {
            def rawValue = Integer.parseInt(descMap.value, 16)
            def batteryPercentage = rawValue / 2          // Zigbee reports in 0.5% steps
            if (batteryPercentage > 100) batteryPercentage = 100
            if (batteryPercentage < 0) batteryPercentage = 0

            def lastBat = device.currentValue("battery")
            def isChange = (lastBat != batteryPercentage)

            logging("Battery: ${batteryPercentage}% (raw:${rawValue})", "info")
            sendEvent(name: "battery", value: batteryPercentage, unit: "%", isStateChange: isChange)
            return
        }
    }

    // Temperature (0x0402)
    if (descMap.cluster == "0402" && descMap.attrId == "0000") {
        def rawValue = Integer.parseInt(descMap.value, 16)
        // Handle signed INT16
        if (rawValue > 32767) rawValue -= 65536
        def tempC = rawValue / 100.0

        // Apply offset (offset is in the selected unit)
        def offset = (settings?.tempOffset ?: 0.0) as Double
        def displayTemp
        def unit

        if (settings?.tempUnit == "F") {
            displayTemp = (tempC * 1.8) + 32.0 + offset
            unit = "°F"
        } else {
            displayTemp = tempC + offset
            unit = "°C"
        }
        displayTemp = Math.round(displayTemp * 10) / 10.0

        def lastTemp = device.currentValue("temperature")
        def isChange = (lastTemp != displayTemp)

        logging("Temperature: ${displayTemp}${unit} (raw C:${tempC})", "info")
        sendEvent(name: "temperature", value: displayTemp, unit: unit, isStateChange: isChange)
        sendEvent(name: "lastUpdate", value: new Date().format("MM/dd/yyyy h:mm a", location.timeZone))
        return
    }

    // Humidity (0x0405)
    if (descMap.cluster == "0405" && descMap.attrId == "0000") {
        def rawValue = Integer.parseInt(descMap.value, 16)
        def humidity = (rawValue / 100.0) + (settings?.humOffset ?: 0.0)
        humidity = Math.round(humidity * 10) / 10.0
        if (humidity > 100) humidity = 100
        if (humidity < 0) humidity = 0

        def lastHum = device.currentValue("humidity")
        def isChange = (lastHum != humidity)

        logging("Humidity: ${humidity}%", "info")
        sendEvent(name: "humidity", value: humidity, unit: "%", isStateChange: isChange)
        return
    }

    // ========== Sonoff FC11 private cluster ==========
    if (descMap.cluster == "FC11") {
        def attrId = descMap.attrId?.toUpperCase()
        def rawHex = descMap.value

        if (!rawHex) {
            logging("FC11 empty value attr:${attrId}", "debug")
            return
        }

        // Individual min/max attributes
        if (attrId in ["2008", "2009", "200B", "200C"]) {
            def rawValue = Integer.parseInt(rawHex, 16)

            // Temperature attributes are signed
            if (attrId in ["2008", "2009"] && rawValue > 32767) {
                rawValue -= 65536
            }

            def valueC = rawValue / 100.0
            def displayValue
            def unit

            if (attrId in ["2008", "2009"]) {          // Temperature
                if (settings?.tempUnit == "F") {
                    displayValue = (valueC * 1.8) + 32.0
                    unit = "°F"
                } else {
                    displayValue = valueC
                    unit = "°C"
                }
                displayValue = Math.round(displayValue * 10) / 10.0

                if (attrId == "2008") {
                    logging("Temp Max (24h): ${displayValue}${unit}", "info")
                    sendEvent(name: "temperatureMax", value: displayValue, unit: unit)
                } else {
                    logging("Temp Min (24h): ${displayValue}${unit}", "info")
                    sendEvent(name: "temperatureMin", value: displayValue, unit: unit)
                }
            } else {                                    // Humidity
                displayValue = Math.round(valueC * 10) / 10.0
                if (attrId == "200B") {
                    logging("Humidity Max (24h): ${displayValue}%", "info")
                    sendEvent(name: "humidityMax", value: displayValue, unit: "%")
                } else {
                    logging("Humidity Min (24h): ${displayValue}%", "info")
                    sendEvent(name: "humidityMin", value: displayValue, unit: "%")
                }
            }
            return
        }

        // Bulk / history reports (attr 0x2010 etc.) – just log at debug
        logging("FC11 attr:${attrId} (bulk/history) ignored", "debug")
        return
    }

    // Ignore common noise clusters
    if (descMap.cluster in ["0013", "0006", "8021", "8038", "8005", "8013", "8032", "8031"]) {
        logging("Ignoring cluster ${descMap.cluster}", "debug")
        return
    }

    logging("Unknown cluster ${descMap.cluster} : ${descMap}", "warn")
}

// ========== LCD Unit Control ==========
// FC11 attribute 0x0007 : 0 = Celsius, 1 = Fahrenheit
// Device must be awake (press the button) for the write to succeed

def setLcdUnitC() {
    logging("Setting LCD to Celsius - wake the device now (press button)", "warn")
    // Manufacturer code 0x1286 (Sonoff/CoolKit)
    def cmds = zigbee.writeAttribute(0xFC11, 0x0007, DataType.UINT16, 0, [mfgCode: 0x1286])
    sendZigbeeCommands(cmds)
}

def setLcdUnitF() {
    logging("Setting LCD to Fahrenheit - wake the device now (press button)", "warn")
    def cmds = zigbee.writeAttribute(0xFC11, 0x0007, DataType.UINT16, 1, [mfgCode: 0x1286])
    sendZigbeeCommands(cmds)
}

// ========== Presence ==========
def checkPresence() {
    if (!state.tries) { state.tries = 0 }
    state.lastPoll = new Date().format("MM/dd/yyyy h:mm a", location.timeZone)
    def checkMin = 45                                       // allow longer for sleepy sensor
    def timeSinceLastCheckin = (now() - (state.lastCheckin ?: 0)) / 1000
    state.lastCheckInMin = timeSinceLastCheckin / 60
    logging("Check Presence its been ${state.lastCheckInMin} mins Timeout:${checkMin} Tries:${state.tries}", "debug")

    if (state.lastCheckInMin <= checkMin) {
        state.tries = 0
        def test = device.currentValue("presence")
        if (test != "present") {
            logging("Creating presence event: present", "info")
            sendEvent(name: "presence", value: "present", descriptionText: "present ${state.version}", isStateChange: true)
        }
        return
    }

    if (state.lastCheckInMin >= checkMin) {
        state.tries = state.tries + 1
        if (state.tries >= 5) {
            def test = device.currentValue("presence")
            if (test != "not present") {
                logging("Creating presence event: not present", "warn")
                sendEvent(name: "presence", value: "not present", descriptionText: "not present ${state.version}", isStateChange: true)
                sendEvent(name: "battery", value: 0, unit: "%", descriptionText: "Simulated ${state.version}", isStateChange: true)
                return
            }
        }
        runIn(2, ping)
        if (state.tries < 4) {
            logging("Recovery in process Last checkin ${state.lastCheckInMin} min ago", "warn")
            runIn(50, checkPresence)
        }
    }
}

// ========== Helpers ==========
void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

void getIcons() {
    state.icon = "<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/sonoff-temp.png' >"
    state.donate = "<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
}

// Logging block v4
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    if (debugLogging) {
        logging("Debug log:off in 3000s", "warn")
        runIn(3000, debugLogOff)
    }
    if (traceLogging) {
        logging("Trace log: off in 1800s", "warn")
        runIn(1800, traceLogOff)
    }
}

void traceLogOff() {
    device.updateSetting("traceLogging", [value: "false", type: "bool"])
    log.trace "${device} : Trace Logging : Automatically Disabled"
}

void debugLogOff() {
    device.updateSetting("debugLogging", [value: "false", type: "bool"])
    log.debug "${device} : Debug Logging : Automatically Disabled"
}

private logging(String message, String level) {
    if (level == "infoBypass") { log.info  "${device} : $message" }
    if (level == "error")      { log.error "${device} : $message" }
    if (level == "warn")       { log.warn  "${device} : $message" }
    if (level == "trace" && traceLogging) { log.trace "${device} : $message" }
    if (level == "debug" && debugLogging) { log.debug "${device} : $message" }
    if (level == "info"  && infoLogging)  { log.info  "${device} : $message" }
}
