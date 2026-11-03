/**
 *  Frigidaire 2.0 Window AC Hubitat driver
 *  Copyright 2026 Tmaster Winnfreenet.com
 *
   v1.0.1  2026-11-02  First working version (beta)
   v1.0.2    ''        Logging Updates 

 
 *  Hubitat driver for Frigidaire 2.0 / Electrolux window & portable ACs.
 *  Control modes: AC Sensor | Thermostat Sensor (external on/off).
 *
 *  API behavior referenced from bm1549's Frigidaire/Home Assistant work
 *  (MIT License): https://github.com/bm1549/frigidaire
 *  
 *  This file is original Groovy by Tmaster. .
======================================================================================================
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
=======================================================================================================
 */


import groovy.transform.Field
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// -----------------------------------------------------------------------------
// Frigidaire / Electrolux cloud – public mobile-app client (not user secrets).
// These identify the request as the official Frigidaire One *Android* app
// (CLIENT_ID = FrigidaireOneApp; Gigya calls also use sdk "Android_6.2.1").
// Same client values are used by reverse-engineered HA/Python integrations.
// User login still requires the account email/password in preferences.
// -----------------------------------------------------------------------------
@Field static final String GLOBAL_API = "https://api.ocp.electrolux.one"
@Field static final String API_KEY = "3BAfxFtCTdGbJ74udWvSe6ZdPugP8GcKz3nSJVfg"
@Field static final String CLIENT_ID = "FrigidaireOneApp"
@Field static final String CLIENT_SECRET = "26SGRupOJaxv4Y1npjBsScjJPuj7f8YTdGxJak3nhAnowCStsBAEzKtrEHsgbqUyh90KFsoty7xXwMNuLYiSEcLqhGQryBM26i435hncaLqj5AuSvWaGNRTACi7ba5yu"
@Field static final String USER_AGENT = "Ktor client"

metadata {
    definition (name: "Frigidaire Window AC (Cloud)", namespace: "tmastersmart", author: "tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/refs/heads/main/frigidaire_2.0.groovy") {

       
        capability "Actuator"
        capability "Sensor"
        capability "Thermostat"
        capability "TemperatureMeasurement"
        capability "Refresh"
        capability "Switch"
        capability "ThermostatSetpoint"
        capability "ThermostatMode"
        capability "ThermostatFanMode"
        capability "ThermostatSetpoint"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatOperatingState"
        
        
        

        command "setFanSpeed", [[name:"Fan Speed*", type:"ENUM", constraints:["auto","low","medium","high"]]]
        command "initialize"
        command "dry"
        command "fanOnly"

        attribute "fanSpeed", "string"
        attribute "applianceId", "string"
        attribute "connection", "string"
        attribute "acAmbient", "number"          // temp at the AC unit
        attribute "controlMode", "string"
        attribute "lastError", "string"
    }

    preferences {
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended info level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "Programming Debug logs" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

    input name: "email", type: "text", title: "Frigidaire account email", required: true
    input name: "password", type: "password", title: "Frigidaire account password", required: true
    input name: "pollInterval", type: "enum", title: "Poll interval (minutes)",options: ["1","2","5","10","15","30"], defaultValue: "5"
    input name: "controlMode", type: "enum", title: "Control Mode",options: ["acSensor": "AC Sensor (unit decides when to run)","thermoSensor": "Thermostat Sensor (force on/off from external)"],defaultValue: "acSensor"
    input name: "externalThermostat", type: "capability.thermostat", title: "External thermostat (for sync / force control)", required: false
    input name: "externalTempSensor", type: "capability.temperatureMeasurement", title: "Optional external temp sensor (overrides thermo temp)", required: false
    input name: "tempOffset", type: "number", title: "Temperature offset (°F) applied when using external sensor", defaultValue: 0
    input name: "forceDeadband", type: "number", title: "Deadband for force on/off (°F)", defaultValue: 1.0


    }
}
def clientVersion() {
    TheVersion="1.0.2"
 if (state.version != TheVersion){    
     logging("Upgrading ! ${state.version} to ${TheVersion}", "warn")
     state.version = TheVersion
  }
}

def installed() {
    logging("Frigidaire AC driver installed", "info")
    initialize()
}

def updated() {
    logging("Updated", "info")
    unschedule()
    initialize()
}

def initialize() {
    sendEvent(name: "supportedThermostatModes", value: ["off","cool","heat","auto"])
    sendEvent(name: "supportedThermostatFanModes", value: ["auto","on","circulate","low","medium","high"])
    sendEvent(name: "controlMode", value: settings.controlMode ?: "acSensor")
    clientVersion()
    unschedule()
    loggingUpdate()
    if (settings.pollInterval) {
        schedule("0 */${settings.pollInterval} * * * ?", "refresh")
    }

    // Drivers: only subscribe (no bare unsubscribe)
    if (settings.externalThermostat) {
        subscribe(settings.externalThermostat, "thermostatMode", externalThermoHandler)
        subscribe(settings.externalThermostat, "coolingSetpoint", externalThermoHandler)
        subscribe(settings.externalThermostat, "heatingSetpoint", externalThermoHandler)
        subscribe(settings.externalThermostat, "temperature", externalThermoHandler)
        subscribe(settings.externalThermostat, "thermostatOperatingState", externalThermoHandler)
    }
    if (settings.externalTempSensor) {
        subscribe(settings.externalTempSensor, "temperature", externalTempHandler)
    }

    runIn(5, "loginAndRefresh")
}

def loginAndRefresh() {
    if (authenticate()) {
        discoverAppliance()
        logging("refresh", "info")
        refresh()
    }
}

// -------------------- Authentication --------------------

private boolean authenticate() {
    if (!settings.email || !settings.password) {
        logging("Email and password required", "warn")
        return false
    }

    if (state.accessToken && state.regionalBaseUrl && state.tokenExpires && state.tokenExpires > now()) {
        logging("Using saved token", "debug")
        return true
    }

    try {
        // ----- 1. Client credentials -----
        def tokenBody = [
            grantType   : "client_credentials",
            clientId    : CLIENT_ID,
            clientSecret: CLIENT_SECRET,
            scope       : ""
        ]
        def tokenResp = httpPostJson(GLOBAL_API, "/one-account-authorization/api/v1/token",
            frigHeaders(false), tokenBody)
        if (!tokenResp?.accessToken) throw new Exception("No accessToken from client_credentials")
        state.tempToken = tokenResp.accessToken

        // ----- 2. Identity providers -----
        def idpList = httpGetJson(GLOBAL_API,
            "/one-account-user/api/v1/identity-providers?brand=frigidaire&countryCode=US",
            frigHeaders(true, state.tempToken))
        def idp = null
        if (idpList instanceof List && idpList.size() > 0) idp = idpList[0]
        else if (idpList instanceof Map) {
            if (idpList.data instanceof List && idpList.data.size() > 0) idp = idpList.data[0]
            else if (idpList.domain) idp = idpList
        }
        if (!idp) throw new Exception("Could not parse identity provider: ${idpList}")

        state.identityDomain  = idp.domain
        state.identityApiKey  = idp.apiKey
        state.regionalBaseUrl = idp.httpRegionalBaseUrl
        if (!state.identityDomain || !state.identityApiKey) {
            throw new Exception("Missing domain/apiKey in idp")
        }
        logging("STEP2 domain=${state.identityDomain} regional=${state.regionalBaseUrl}", "debug")


        // ----- 3. getIDs -----
        def nonce = "${(long)(now()/1000)}_-${new Random().nextInt(Integer.MAX_VALUE)}"
        def idsBody = [
            apiKey         : state.identityApiKey,
            format         : "json",
            httpStatusCodes: "false",
            nonce          : nonce,
            sdk            : "Android_6.2.1",
            targetEnv      : "mobile"
        ]
def idsRaw = httpPostForm("https://socialize.${state.identityDomain}", "/socialize.getIDs",
            authHeaders(), idsBody)
        def idsResp = asMap(idsRaw)
        def gmid = idsResp?.gmid ?: extractField(idsRaw, "gmid")
        def ucid = idsResp?.ucid ?: extractField(idsRaw, "ucid")
        if (!gmid || !ucid) throw new Exception("getIDs missing gmid/ucid")
       
        logging("STEP3 OK gmid=${gmid} ucid=${ucid}", "debug")
        // ----- 4. login -----
        nonce = "${(long)(now()/1000)}_-${new Random().nextInt(Integer.MAX_VALUE)}"
        def loginBody = [
            apiKey         : state.identityApiKey,
            format         : "json",
            gmid           : gmid,
            httpStatusCodes: "false",
            loginID        : settings.email,
            nonce          : nonce,
            password       : settings.password,
            sdk            : "Android_6.2.1",
            targetEnv      : "mobile",
            ucid           : ucid
        ]
def loginRaw = httpPostForm("https://accounts.${state.identityDomain}", "/accounts.login",
            authHeaders(), loginBody)
        def loginResp = asMap(loginRaw)
        def sessionToken  = loginResp?.sessionInfo?.sessionToken ?: extractField(loginRaw, "sessionToken")
        def sessionSecret = loginResp?.sessionInfo?.sessionSecret ?: extractField(loginRaw, "sessionSecret")
        logging("STEP4 tokenLen=${sessionToken?.length()} secretLen=${sessionSecret?.length()} err=${loginResp?.errorCode ?: extractField(loginRaw,'errorCode')}", "debug")

        if (!sessionToken || !sessionSecret) {
            throw new Exception("Login failed – check email/password")
        }

        // ----- 5. getJWT -----
        nonce = "${(long)(now()/1000)}_-${new Random().nextInt(Integer.MAX_VALUE)}"
        def jwtParams = [
            apiKey         : state.identityApiKey,
            fields         : "country",
            format         : "json",
            gmid           : gmid,
            httpStatusCodes: "false",
            nonce          : nonce,
            oauth_token    : sessionToken,
            sdk            : "Android_6.2.1",
            targetEnv      : "mobile",
            timestamp      : "${(long)(now()/1000)}",
            ucid           : ucid
        ]
        def sig = gigyaSignature(sessionSecret, "POST",
            "https://accounts.${state.identityDomain}/accounts.getJWT", jwtParams)
        if (!sig) throw new Exception("gigyaSignature returned null")
        jwtParams.sig = sig

        def jwtRaw = httpPostForm("https://accounts.${state.identityDomain}", "/accounts.getJWT",
            authHeaders(), jwtParams)
        def jwtResp = asMap(jwtRaw)
        def idToken = jwtResp?.id_token ?: jwtResp?.idToken ?: extractField(jwtRaw, "id_token")
        if (!idToken) idToken = extractField(jwtRaw, "idToken")
        // JWT pattern fallback
        if (!idToken) {
            def t = jwtRaw?.toString()
            def m = (t =~ /eyJ[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+/)
            if (m.find()) idToken = m.group(0)
        }
        def jwtErr = jwtResp?.errorCode ?: extractField(jwtRaw, "errorCode")
        def jwtReason = jwtResp?.statusReason ?: extractField(jwtRaw, "statusReason")
        logging("STEP5 err=${jwtErr} reason=${jwtReason} idToken=${idToken ? 'YES' : 'NULL'}", "debug")


        if (!idToken) throw new Exception("getJWT failed errorCode=${jwtErr} reason=${jwtReason}")

        // ----- 6. token exchange -----
        def exchangeBody = [
            grantType: "urn:ietf:params:oauth:grant-type:token-exchange",
            clientId : CLIENT_ID,
            idToken  : idToken,
            scope    : ""
        ]
        def finalToken = httpPostJson(state.regionalBaseUrl, "/one-account-authorization/api/v1/token",
            frigHeaders(false), exchangeBody)
        if (!finalToken?.accessToken) throw new Exception("Token exchange failed: ${finalToken}")

        state.accessToken = finalToken.accessToken
        state.tokenExpires = now() + (12L * 60L * 60L * 1000L)
        state.remove("tempToken")
        sendEvent(name: "lastError", value: "OK")
        logging("Authentication successful", "info")

        return true

    } catch (Exception e) {
        logging("Auth failed: ${e.message}", "error")

        state.accessToken = null
        sendEvent(name: "lastError", value: e.message?.take(120))
        return false
    }
}

// -------------------- Discovery & status --------------------

def discoverAppliance() {
    if (!authenticate()) return
    def list = httpGetJson(state.regionalBaseUrl,
        "/appliance/api/v2/appliances?includeMetadata=true",
        frigHeaders(true))
    if (!(list instanceof List) || list.isEmpty()) {
        logging("No appliances found on account", "error")
        return
    }
    // Prefer first AC-type appliance
    def ac = list.find { rec ->
        def type = rec?.applianceData?.modelName ?: rec?.applianceData?.applianceType
        type in ["AC", "Panther", "Telica"] || (rec?.properties?.reported?.targetTemperatureF != null)
    } ?: list[0]

    state.applianceId = ac.applianceId
    sendEvent(name: "applianceId", value: state.applianceId)

    logging("Using appliance ID: ${state.applianceId}", "debug")

    parseApplianceState(ac)
}

def refresh() {
    if (!authenticate()) return
    if (!state.applianceId) {
        discoverAppliance()
        return
    }
    def list = httpGetJson(state.regionalBaseUrl,
        "/appliance/api/v2/appliances?includeMetadata=true",
        frigHeaders(true))
    def rec = list?.find { it.applianceId == state.applianceId }
    if (rec) parseApplianceState(rec)
    else logging("Appliance ${state.applianceId} not found on refresh", "error")

}

private void parseApplianceState(Map rec) {
    def r = rec?.properties?.reported ?: [:]
    def mode = (r.mode ?: r.modeState ?: "OFF").toString().toUpperCase()
    def stateVal = (r.applianceState ?: "").toString().toUpperCase()
    def ambient = r.ambientTemperatureF ?: r.ambientTemperatureC
    def target = r.targetTemperatureF ?: r.targetTemperatureC
    def fan = (r.fanSpeedSetting ?: r.fanSpeedState ?: "").toString().toUpperCase()

def tMode = "off"
switch (mode) {
    case "COOL":    tMode = "cool"; break
    case "HEAT":    tMode = "heat"; break
    case "ECO":     tMode = "auto"; break   // show as Auto on Hubitat
    case "FANONLY": tMode = "fan"; break
    case "DRY":     tMode = "dry"; break
    case "AUTO":    tMode = "auto"; break
    default:
        tMode = (stateVal == "OFF" || mode == "OFF") ? "off" : "cool"
}

    logging("Mode:${tMode}", "info")
    sendEvent(name: "thermostatMode", value: tMode)
    sendEvent(name: "thermostatOperatingState", value: (stateVal == "RUNNING" ? (tMode == "heat" ? "heating" : "cooling") : "idle"))
    if (target != null) {
        sendEvent(name: "coolingSetpoint", value: target as Integer, unit: "F")
        sendEvent(name: "heatingSetpoint", value: target as Integer, unit: "F")
        sendEvent(name: "thermostatSetpoint", value: target as Integer, unit: "F")
        logging("SetPoint:${target}", "info")

    }
    if (ambient != null) {
        sendEvent(name: "acAmbient", value: ambient, unit: "F")
        logging("Temp: ${ambient}", "info")
        // By default show AC ambient; external mode can override displayed temperature
        if ((settings.controlMode ?: "acSensor") == "acSensor") {
            sendEvent(name: "temperature", value: ambient, unit: "F")
        }
    }
    def fanMap = [LOW:"low", MIDDLE:"medium", HIGH:"high", AUTO:"auto"]
    sendEvent(name: "thermostatFanMode", value: fanMap[fan] ?: "auto")
    sendEvent(name: "fanSpeed", value: fanMap[fan] ?: fan?.toLowerCase())
    sendEvent(name: "switch", value: (tMode == "off" ? "off" : "on"))
    sendEvent(name: "connection", value: (rec?.connectionState ?: r.networkInterface?.linkQualityIndicator ?: "unknown").toString().toLowerCase())
}

// -------------------- Commands --------------------

def setThermostatMode(String mode) {
    mode = mode?.toLowerCase()
    logging("Set Mode: ${mode}", "info")
    switch (mode) {
        case "off":  off(); break
        case "cool": setMode("ECO"); break
        case "heat": setMode("HEAT"); break
        case "auto": setMode("ECO"); break   // Auto on dashboard = Eco on unit
        case "fan":  setMode("FANONLY"); break
//       case "dry":  setMode("DRY"); break
//        case "eco":  setMode("ECO"); break   // optional, if something still sends eco
        default: logging("Unknown Mode: ${mode}", "warn")
    }
}

def setCoolingSetpoint(Number t) { setTemperature(t as int) }
def setHeatingSetpoint(Number t) { setTemperature(t as int) }
def setThermostatFanMode(String m) {
    def map = [auto:"AUTO", on:"HIGH", circulate:"LOW", low:"LOW", medium:"MIDDLE", high:"HIGH"]
    setFanSpeedRaw(map[m?.toLowerCase()] ?: "AUTO")
    logging("Set FAN: ${m}", "info")
}

def on()  { 
    setPower(true);
    if (device.currentValue("thermostatMode") == "off") setMode("ECO") 
    logging("ON ECO", "info")
}
def off() { 
    setPower(false);
    sendEvent(name: "thermostatMode", value: "off") 
    logging("OFF", "info")
}
def cool() { setThermostatMode("cool") }
def heat() { setThermostatMode("heat") }
def emergencyHeat() { heat() }
def auto() { setThermostatMode("auto") }
def fanOn() { setThermostatFanMode("on") }
def fanAuto() { setThermostatFanMode("auto") }
def fanCirculate() { setThermostatFanMode("circulate") }
def eco() { setThermostatMode("eco") }
def dry() { setThermostatMode("dry") }
def fanOnly() { setThermostatMode("fan") }

def setFanSpeed(String speed) {
    def map = [auto:"AUTO", low:"LOW", medium:"MIDDLE", high:"HIGH"]
    setFanSpeedRaw(map[speed?.toLowerCase()] ?: "AUTO")
    logging("FAN ${speed}", "info")
}

// -------------------- Low-level API commands --------------------

private void setPower(boolean on) {
    sendCommand([executeCommand: on ? "ON" : "OFF"])
}

private void setMode(String mode) {
    // Power on first (API quirk for ACs)
    if (mode != "OFF") sendCommand([executeCommand: "ON"])
    sendCommand([mode: mode])
}

private void setTemperature(int t) {
    t = Math.max(60, Math.min(90, t))
    sendCommand([temperatureRepresentation: "FAHRENHEIT", targetTemperatureF: t])
}

private void setFanSpeedRaw(String speed) {
    sendCommand([fanSpeedSetting: speed])
}

private void sendCommand(Map body) {
    if (!authenticate() || !state.applianceId) {
        logging("Cannot send command – not authenticated or no applianceId", "warn")

        return
    }
    def path = "/appliance/api/v2/appliances/${state.applianceId}/command"
    try {
        httpPutJson(state.regionalBaseUrl, path, frigHeaders(true), body)
        logging("Command sent: ${body}", "debug") 
        runIn(3, "refresh")   // let cloud catch up
    } catch (Exception e) {
        logging("Command failed: ${e.message}", "warn") 

    }
}

// -------------------- External thermostat logic --------------------

def externalThermoHandler(evt) {
    if ((settings.controlMode ?: "acSensor") != "thermoSensor") {
        // Still mirror setpoint/mode if user changes the wall thermostat
        mirrorFromExternal()
        return
    }
    forceFromExternal()
}

def externalTempHandler(evt) {
    if ((settings.controlMode ?: "acSensor") == "thermoSensor") {
        forceFromExternal()
    } else {
        // Just update displayed temperature
        def t = evt.numericValue
        if (t != null) {
            def offset = (settings.tempOffset ?: 0) as double
            sendEvent(name: "temperature", value: (t + offset), unit: "F")
        }
    }
}

private void mirrorFromExternal() {
    if (!settings.externalThermostat) return
    def mode = settings.externalThermostat.currentValue("thermostatMode")
    def sp = settings.externalThermostat.currentValue("coolingSetpoint")
    if (mode) setThermostatMode(mode)
    if (sp != null) setCoolingSetpoint(sp as Number)
}

private void forceFromExternal() {
    if (!settings.externalThermostat) return
    def mode = settings.externalThermostat.currentValue("thermostatMode")?.toLowerCase()
    def opState = settings.externalThermostat.currentValue("thermostatOperatingState")?.toLowerCase()
    def sp = settings.externalThermostat.currentValue("coolingSetpoint") as Double
    def temp = settings.externalTempSensor?.currentValue("temperature") as Double
    if (temp == null) temp = settings.externalThermostat.currentValue("temperature") as Double
    if (temp != null) {
        def offset = (settings.tempOffset ?: 0) as double
        temp += offset
        sendEvent(name: "temperature", value: temp, unit: "F")
    }

    // Keep AC setpoint roughly in sync
    if (sp != null) setCoolingSetpoint(sp)

    def deadband = (settings.forceDeadband ?: 1.0) as double
    boolean shouldCool = false
    if (mode in ["cool", "auto"]) {
        if (opState in ["cooling", "pending cool"]) shouldCool = true
        else if (temp != null && sp != null && temp > sp + deadband) shouldCool = true
        else if (temp != null && sp != null && temp < sp - deadband) shouldCool = false
    }

    if (shouldCool) {
        setMode("COOL")
    } else {
        // Turn compressor off but leave fan if desired – simplest is full off or fan only
        setPower(false)
    }
}

// -------------------- HTTP helpers --------------------

private Map frigHeaders(boolean bearer, String token = null) {
    def t = token ?: state.accessToken
    def h = [
        "x-api-key"   : API_KEY,
        "Accept"      : "application/json",
        "User-Agent"  : USER_AGENT,
        "Content-Type": "application/json"
    ]
    h["Authorization"] = bearer && t ? "Bearer $t" : "Bearer"
    return h
}

private Map authHeaders() {
    ["User-Agent": "Dalvik/2.1.0 (Linux; U; Android 12)", "Content-Type": "application/x-www-form-urlencoded", "Accept-Encoding": "gzip"]
}

private def httpGetJson(String base, String path, Map headers) {
    def params = [uri: "${base}${path}", headers: headers, contentType: "application/json", timeout: 30]
    def result = null
    httpGet(params) { resp ->
        if (resp.status == 200) result = resp.data
        else throw new Exception("GET ${path} → ${resp.status}")
    }
    return result
}

private def httpPostJson(String base, String path, Map headers, Map body) {
    def params = [uri: "${base}${path}", headers: headers, contentType: "application/json", body: body, timeout: 30]
    def result = null
    httpPost(params) { resp ->
        if (resp.status in [200, 201]) result = resp.data
        else throw new Exception("POST ${path} → ${resp.status}")
    }
    return result
}

private def httpPutJson(String base, String path, Map headers, Map body) {
    def params = [uri: "${base}${path}", headers: headers, contentType: "application/json", body: body, timeout: 30]
    httpPut(params) { resp ->
        if (!(resp.status in [200, 201, 204])) throw new Exception("PUT ${path} → ${resp.status}")
    }
}

private def httpPostForm(String base, String path, Map headers, Map body) {
    def form = body.collect { k, v ->
        "${URLEncoder.encode(k.toString(),'UTF-8')}=${URLEncoder.encode(v.toString(),'UTF-8')}"
    }.join("&")

    // Ask for generic content so Hubitat is less aggressive about JSON typing
    def h = new HashMap(headers)
    h["Accept"] = "*/*"

    def params = [
        uri                : "${base}${path}",
        headers            : h,
        contentType        : "application/x-www-form-urlencoded",
        requestContentType : "application/x-www-form-urlencoded",
        body               : form,
        timeout            : 30,
        ignoreSSLIssues    : true
    ]

    def result = null
    httpPost(params) { resp ->
        logging("HTTP FORM ${path} status=${resp.status} data=${resp.data}", "warn")  
        if (resp.status != 200) {
            throw new Exception("FORM POST ${path} → ${resp.status} body=${resp.data}")
        }
        result = resp.data
    }
    return result
}
private def asMap(def raw) {
    if (raw == null) return [:]
    // Try JSON round-trip
    try {
        def json = groovy.json.JsonOutput.toJson(raw)
        def parsed = new groovy.json.JsonSlurper().parseText(json)
        if (parsed instanceof List && parsed.size() > 0) parsed = parsed[0]
        if (parsed instanceof Map) return parsed
    } catch (ignored) {}
    try {
        def json = groovy.json.JsonOutput.toJson(raw[0])
        def parsed = new groovy.json.JsonSlurper().parseText(json)
        if (parsed instanceof Map) return parsed
    } catch (ignored) {}
    return [:]
}

// Pull a field from Hubitat's weird response objects via regex on toString()
private String extractField(def raw, String name) {
    def text = raw?.toString()
    if (!text) return null
    // Prefer proper JSON: "field": "value"
    def m = (text =~ /"${name}"\s*:\s*"([^"]*)"/)
    if (m.find()) return m.group(1)
    // Fallback Groovy toString: field=value or field:value
    m = (text =~ /(?:^|[{\[\s,])${name}\s*[=:]\s*"?([^",}\]\s]+)"?/)
    if (m.find()) return m.group(1)
    return null
}

private String gigyaSignature(String secret, String method, String url, Map params) {
    // OAuth-style: sort keys, encode each key/value once, then encode the joined string
    def keys = params.keySet().findAll { params[it] != null && params[it].toString() != "" }.sort()
    def pairs = keys.collect { k ->
        "${oauthEnc(k)}=${oauthEnc(params[k].toString())}"
    }
    def query = pairs.join("&")


// Only scheme + host are lowercased; PATH case must be preserved (getJWT ≠ getjwt)
def uri = new URI(url)
def normUrl = "${uri.scheme.toLowerCase()}://${uri.host.toLowerCase()}${uri.path}".split("\\?")[0]  
    def base = "${method.toUpperCase()}&${oauthEnc(normUrl)}&${oauthEnc(query)}"    
    def head = base.length() > 60 ? base.substring(0, 60) : base
    def tail = base.length() > 80 ? base.substring(base.length() - 80) : base

    logging("SIG baseLen=${base.length()} head=${head}", "warn") 
    logging("SIG tail=${tail}", "warn")   
    // Session secret is standard base64
    byte[] keyBytes
    try {
        keyBytes = secret.decodeBase64()
    } catch (Exception e) {
        // try url-safe alphabet
        keyBytes = secret.replace('-', '+').replace('_', '/').decodeBase64()
    }

 logging("SIG keyLen=${keyBytes?.length} secretLen=${secret?.length()}", "warn") 
    Mac mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"))
    byte[] rawMac = mac.doFinal(base.getBytes("UTF-8"))
    return rawMac.encodeBase64().toString().replace('+', '-').replace('/', '_')
}

private String oauthEnc(String s) {
    if (s == null) return ""
    // RFC 3986-ish: URLEncoder then fix the chars OAuth expects
    return URLEncoder.encode(s, "UTF-8")
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")
}

private void logError(String msg) {
    logging("${msg}", "warn") 
    
    sendEvent(name: "lastError", value: msg.take(100))
}

// Logging block  v4.1
// 4 mode logging mod
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




