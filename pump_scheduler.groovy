/**
 *  Pool / Spa / Drainage Pump Scheduler (c) 2026 All rights reserved
 * 
 *  For swiming pool pumps that dont have a timer,Check your pools specs
 *  On how long it should run per day and its max run time.
 *
 *  For drainage pumps. If the area cant be drained in several hrs the pump 
 *  will over heat and trip the reset so a cool down time is needed. 
 *  Set max run time and max time per day.
 *
 *  Air pumps for septic systems. Once that expensive controler fails
 *  you can just add a relay and use your hub to control it.
 *  Check max air time per day and max ontime and max off time. 
 *
 *
 * v1.2 7/19/2026   Int version
 * v1.5 7/22/2026   Much debuging changes in monitor and more debug code
 * v1.6 7/23/2026   Insert into Hubitat Package Manager
 * v1.8 7/24/2026   Bugs fixed/ Manual buttons added.
 * v1.9 7/25/2026   Log fixes and status page improvements
 * v2.0.0           Manual start stop added
 * v2.0.1           Adjusting monitor. 
 * v2.0.3           Reduced monitor scheduling to 30m. 
 */

definition(
    name: "Pump Scheduler",
    namespace: "tmastersmart",
    author: "Tmaster",
    importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/pump_scheduler.groovy",
    description: "Smart scheduler for pool pumps, spa pumps, septic aerators, drainage pumps, and other motorized equipment. Supports daily runtime targets, maximum cycle length, cooldown periods, sunrise/sunset scheduling, monitoring, and automatic recovery after hub reboots or app updates.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)
preferences {
    page(name: "mainPage")
    page(name: "setupPage")
    page(name: "defaultsPage")
    page(name: "advancedPage")
}

def version() { "2.0.4" }  
def clientVersion() {
    if (state.version != version()) {
        logging("Pump - ${pump.displayName} Scheduler Updated to v${version()}","warn")
        state.version = version()
    }
}



def mainPage() {
    dynamicPage(name: "mainPage", title: "Pump Scheduler",install: true, uninstall: true) {
    
        section("Diagnostics -  v${state.version}") {

    def runtimeHours = (state.totalRuntimeToday ?: 0) / 3600

    // Include current cycle runtime if running
    if (state.isRunning && state.currentCycleStart) {
        runtimeHours += (now() - state.currentCycleStart) / 3600000
    }

    def targetHours = dailyHours ?: 0
    def remainingHours = Math.max(0, targetHours - runtimeHours)

    def runtimeStr   = String.format("%.2f", runtimeHours)
    def targetStr    = String.format("%.2f", targetHours)
    def remainingStr = String.format("%.2f", remainingHours)

    // Progress Bar
    def percent = targetHours ? Math.min(100, (runtimeHours / targetHours) * 100) : 0
    def percentInt = percent.toInteger()

    int filled = Math.round(percentInt / 10.0)
    int empty = 10 - filled

    def progressBar = ("█" * filled) + ("░" * empty)

// Determine current mode
def mode = "⏳ Waiting for Start"

if (state.manualMode) {
    mode = "🔧 Manual Mode"
}
else if (state.schedulerPaused) {
    mode = "⛔ Scheduler Paused"
}
else if (state.isRunning) {
    mode = "🟢 Running"
}
else if (state.nextStartTime && remainingHours > 0) {
    mode = "⏸ Cooldown"
}
else if (remainingHours <= 0) {
    mode = "✅ Daily Complete"
}

    def status = ""

    status += "<b>Status</b> - "
    status += "<b>Mode:</b> ${mode} "
    status += "<b>Pump:</b> ${pump?.displayName ?: "Not Selected"}<br>"

    status += "<b>Today's Progress</b> "
    status += "${progressBar} ${percentInt}% "
    status += "(${runtimeStr} of ${targetStr} hrs) "

    status += "<b>Remaining:</b> ${remainingStr} hrs "

    status += "<b>Cycles:</b> ${state.cyclesToday ?: 0}"
    if (maxCyclesPerDay) {
        status += " / ${maxCyclesPerDay}"
    }
    status += "<br>"

    if (state.currentCycleStart) {
        status += "<b>Started:</b> " +
            new Date(state.currentCycleStart).format("h:mm:ss a", location.timeZone) + "<br>"
    }

    if (state.stopTime) {
        status += "<b>Stops:</b> " +
            new Date(state.stopTime).format("h:mm:ss a", location.timeZone) + "<br>"
    }

    if (state.nextStartTime) {
        status += "<b>Next Start:</b> " +
            new Date(state.nextStartTime).format("h:mm:ss a", location.timeZone) + "<br>"
    }

    if (startOption == "Specific Time" && startTime) {
        status += "<b>Daily Start:</b> " +
            timeToday(startTime, location.timeZone).format("h:mm a", location.timeZone)
    }
    else if (startOption == "Sunrise") {
        status += "<b>Daily Start:</b> Sunrise"
    }
    else if (startOption == "Sunset") {
        status += "<b>Daily Start:</b> Sunset"
    }

    paragraph status
}
        
    section("Maintenance") {
    paragraph "Recovery tools for the scheduler."
    input "forceMonitor", "button",  title: "Run Monitor"
    input "restartMonitor", "button",title: "Restart Monitor"
    input "restartDay", "button",    title: "Restart Today's Schedule"
    input "manualStart", "button", title: "▶ Start Pump"
    input "manualStop", "button", title: "■ Stop Pump"
    input "stopScheduler", "button", title: "⏹ Stop Scheduler"
    input "resumeScheduler", "button", title: "↻ Resume Scheduler"
    input "refreshStatus", "button", title: "Refresh Status"   
        
       
  }

    section("Menu") {
    href "setupPage",   title: "Configuration",     description: "Pump settings"
    href "defaultsPage",title: "Equipment Defaults",description: "Load preset values"
        }
    footer()   
    }
}

def appButtonHandler(btn) {
    switch(btn) {
        case "restartDay":
            logging("Pump - ${pump.displayName} Manual daily restart requested", "warn")
            unschedule("monitorPump")
            state.lastRunDate = null
            state.totalRuntimeToday = 0
            state.cyclesToday = 0
            state.stopTime = null
            state.nextStartTime = null
            state.currentCycleStart = null
            state.isRunning = false
            startDailySchedule()
            return mainPage()
        
        case "restartMonitor":
            logging("Pump - ${pump.displayName} Manual monitor restart requested", "warn")
            restoreMonitor()
            return mainPage()
        
       case "forceMonitor":
            logging("Pump - ${pump.displayName} Force monitor requested", "warn")
            monitorPump()
            return mainPage()   
        
        case "manualStart":
    logging("Pump - ${pump.displayName} Manual START requested", "warn")

    unschedule()
    pump.on()

    state.isRunning = false
    state.stopTime = null
    state.nextStartTime = null
    state.currentCycleStart = null
    state.manualMode = true

    logging("Pump - ${pump.displayName} Scheduler disabled - Manual mode", "info")
        return mainPage()


case "manualStop":
    logging("Pump - ${pump.displayName} Manual STOP requested", "warn")

    unschedule("monitorPump")
    unschedule("startPumpCycle")
    pump.off()

    state.isRunning = false
    state.stopTime = null
    state.nextStartTime = null
    state.currentCycleStart = null
    state.manualMode = true

    logging("Pump - ${pump.displayName} Scheduler disabled - Manual mode", "info")
        return mainPage()


case "stopScheduler":
    logging("Pump - ${pump.displayName} Scheduler stopped", "warn")

    unschedule("monitorPump")
    unschedule("startPumpCycle")
    unschedule("stopPumpCycle")
    unschedule("startDailySchedule")

    state.schedulerPaused = true
    state.manualMode = false
    state.stopTime = null
    state.nextStartTime = null
    state.currentCycleStart = null
    // Make sure the pump is off
    if (pump.currentSwitch == "on") {
        pump.off()
        logging("Pump - ${pump.displayName} Pump turned OFF by stop scheduler ", "info")
    }
        return mainPage()


case "resumeScheduler":
    logging("Pump - ${pump.displayName} Scheduler resumed", "warn")

    state.schedulerPaused = false
    state.manualMode = false
    state.nextStartTime = now() + 2
    scheduleStartTime()
    restoreMonitor()
    runIn(1, "monitorPump")
    return mainPage()
        
case "refreshStatus":
    return mainPage()        
        
    }
}


def defaultsPage() {

    dynamicPage(name: "defaultsPage", title: "Equipment Defaults", nextPage: "mainPage", uninstall: false, install: false) {

        section("Choose Equipment") {
           input "equipmentPreset","enum",title: "Preset",submitOnChange: true,options:["Pool Pump","Spa Pump","Septic Aerator","Drainage Pump","Other"]
        }

        if(equipmentPreset){
            section("Load Defaults"){
               paragraph "Press Done to save these defaults."

                if(equipmentPreset=="Pool Pump"){
                    app.updateSetting("dailyHours",[value:"8",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"4",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"30",type:"number"])
                }

                if(equipmentPreset=="Spa Pump"){
                    app.updateSetting("dailyHours",[value:"2",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"2",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"15",type:"number"])
                }

                if(equipmentPreset=="Septic Aerator"){
                    app.updateSetting("dailyHours",[value:"12",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"4",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"60",type:"number"])
                }

                if(equipmentPreset=="Drainage Pump"){
                    app.updateSetting("dailyHours",[value:"18",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"4",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"30",type:"number"])
                }

            }
        }
    }
}


def setupPage() {
    dynamicPage(name: "setupPage", title: "Pump Scheduler Configuration",  nextPage: "mainPage") {

        section("Pump") {
           input "pump","capability.switch",title: "Select Pump Device",description: "Choose the switch that controls your pump.",required: true
           input "deviceType","enum",title: "Pump Type",options: ["Pool Pump","Spa Pump","Drainage Pump","Septic Aerator","Other"],description: "Used for presets and status messages.",required: true
        }
        section("Daily Schedule") {
            input "dailyHours","decimal",title: "Total Daily Runtime (hours)",description: "Total amount of time the pump should run each day.",defaultValue: 8.0,required: true
            input "maxRunPerCycle","decimal",title: "Maximum Continuous Runtime (hours)",description: "Longest the pump may run before taking a cooldown break.",defaultValue: 4.0,required: true
            input "startOption","enum",title: "Daily Start Time",options: ["Specific Time","Sunrise","Sunset"],description: "Choose when the first run of the day begins.",defaultValue: "Specific Time",required: true
            input "startTime","time",title: "Start Time",description: "Only used when 'Specific Time' is selected.",required: false
        }
        section("Cooldown") {
            input "cooldownMinutes","number",title: "Cooldown Between Cycles (minutes)",description: "Time the pump remains OFF before the next run begins.",defaultValue: 30,required: true
        }
        section("Limits") {
            input "maxCyclesPerDay","number",title: "Maximum Cycles Per Day",description: "Number of run cycles allowed each day. Set to 0 for unlimited cycles.",defaultValue: 0,required: true
}
//   log.info "Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]"
    
        section("Logging") {
                paragraph "Debug and Trace logging automatically turn off after a period of time unless 'Lock' is enabled."

            input "infoLogging","bool",title: "Enable Info Logging",defaultValue: (settings.infoLogging != null ? settings.infoLogging : true)
            input "debugLogging","bool",title: "Enable Debug Logging",defaultValue: (settings.debugLogging != null ? settings.debugLogging : false)
            input "traceLogging","bool",title: "Enable Trace Logging",defaultValue: (settings.traceLogging != null ? settings.traceLogging : false)
            input "lockLogging","bool",title: "Lock",defaultValue: (settings.lockLogging != null ? settings.lockLogging : false)
}
    footer()         

    }
}




def installed() {
    clientVersion()
    logging("Pump - ${pump.displayName} Scheduler Installed","info")
    initialize()
    loggingUpdate()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
    restoreMonitor()
    logging("Pump - ${pump.displayName} Scheduler Updated - ${dailyHours} hrs/day | Max ${maxRunPerCycle} hrs per cycle","trace")    
    loggingUpdate()
}


def restoreMonitor() {

    def runtime = state.totalRuntimeToday ?: 0
    def target = dailyHours * 3600
    def remaining = target - runtime

    unschedule("monitorPump")

    if (state.isRunning) {

        schedule("0 0/25 * * * ?", "monitorPump")
        logging("Pump - ${pump.displayName} Monitor restored (Pump Running)", "info")

    }
    else if (remaining <= 60) {

        state.nextStartTime = null
        logging("Pump - ${pump.displayName} Daily runtime complete Runtime=${String.format('%.2f', runtime/3600)}h","info")

    }
    else if (state.nextStartTime) {

        schedule("0 0/25 * * * ?", "monitorPump")
        logging("Pump - ${pump.displayName} Monitor restored (Cooldown active)","info")
        logging("Pump - ${pump.displayName} NextStart=${new Date(state.nextStartTime).format('HH:mm:ss', location.timeZone)} Remaining=${String.format('%.2f', remaining/3600)}h","debug")

    }
    else {

        logging("Pump - ${pump.displayName} Runtime remaining (${String.format('%.2f', remaining/3600)}h) but no NextStart exists","warn")

        state.nextStartTime = now() + (cooldownMinutes * 60 * 1000)

        schedule("0 0/25 * * * ?", "monitorPump")

        logging("Pump - ${pump.displayName} Rebuilt cooldown. NextStart=${new Date(state.nextStartTime)}","info")
    }
}


def initialize() {
    if (state.totalRuntimeToday == null){ state.totalRuntimeToday = 0.0 }
          if (state.cyclesToday == null){ state.cyclesToday = 0 }
           if (state.isRunning == null){  state.isRunning = false }
//    logging("Pump - ${pump.displayName} Runtime ${state.totalRuntimeToday} Cycles ${state.cyclesToday}", "debug")
    clientVersion()
    scheduleStartTime()
    logging("Pump - ${pump.displayName} Scheduler initialized - ${dailyHours} hrs/day | Max ${maxRunPerCycle} hrs per cycle", "info")
}


def scheduleStartTime() {
    // Remove any previous start schedule
    unschedule("startDailySchedule")
    switch(startOption) {
        case "Sunrise":
            subscribe(location, "sunriseTime", startDailySchedule)
            logging("Pump - ${pump.displayName} Scheduled to start at Sunrise", "info")
            break
        case "Sunset":
            subscribe(location, "sunsetTime", startDailySchedule)
            logging("Pump - ${pump.displayName} Scheduled to start at Sunset", "info")
            break
        case "Specific Time":
            if (startTime) {
                schedule(startTime, startDailySchedule)
                logging("Pump - ${pump.displayName} Scheduled to start at ${startTime}", "info")
            }
            break
        default:
            logging("Pump - ${pump.displayName} No valid start schedule configured", "warn")
            break
    }
}

def startDailySchedule(evt = null) {
    // Always make sure the monitor is running
    state.manualMode = false
    unschedule("monitorPump")
    schedule("0 0/25 * * * ?", "monitorPump")
    logging("Pump - ${pump.displayName} Monitor reset", "debug")
    
 //   runIn(60, "monitorPump") // lets not do this now
    
    def today = new Date().format("yyyy-MM-dd", location.timeZone)

    if (state.lastRunDate == today) {
        logging("Pump - ${pump.displayName} Today's schedule already started. Runtime ${state.totalRuntimeToday} Cycles ${state.cyclesToday}","warn")
        return
    }
    state.monitorCount =0
    state.lastRunDate = today
    state.totalRuntimeToday = 0.0
    state.cyclesToday = 0
    state.isRunning = false
    state.stopTime = null
    state.nextStartTime = null
    state.currentCycleStart = null
    logging("Pump - ${pump.displayName} Daily cycle started","info")
    startPumpCycle()
    
}



def startPumpCycle() {
    if (state.isRunning) return
if (maxCyclesPerDay && state.cyclesToday >= maxCyclesPerDay) {
    state.nextStartTime = null
    unschedule("monitorPump")
    logging("Pump - ${pump.displayName} Max cycles reached! Monitor:off Increase cycles if needed. ${state.cyclesToday}", "info")
    return
}

    
    def remaining = (dailyHours * 3600) - state.totalRuntimeToday
    if (remaining <= 0) return
    
    def runSeconds = Math.min(remaining, maxRunPerCycle * 3600)
    
    pump.on()
    state.isRunning = true
    state.currentCycleStart = now()
    state.stopTime = now() + (runSeconds.toInteger() * 1000)
    state.nextStartTime = null
    state.cyclesToday++
    state.lastStopTime = null    
    def debugRuntime = state.totalRuntimeToday ?: 0
    def debugHours = debugRuntime / 3600
    def next = "Next event NULL"

    if (state.nextStartTime) {
    next = "Next Start:" + new Date(state.nextStartTime).format("h:mm a", location.timeZone)
    }
    if (state.stopTime) {
    next = "Next Stop:" + new Date(state.stopTime).format("h:mm a", location.timeZone)
    }
    
    runIn(runSeconds.toInteger(), stopPumpCycle)

    logging("Pump - ${pump.displayName} turned on. Runtime:${String.format('%.3f', debugHours)} hrs Cycle#${state.cyclesToday} ${next}","info")
}



def stopPumpCycle() {
    if (!state.isRunning){
    logging("Pump - ${pump.displayName} Off time but was not running.","warn")
    return
    }
    state.isRunning = false 
    def actualRun = (now() - state.currentCycleStart) / 1000
    state.totalRuntimeToday += actualRun
    state.stopTime = null
    state.currentCycleStart = null
    state.lastStopTime = now()
    
    pump.off()  // do after to make sure theres no bounce
    def debugRuntime = state.totalRuntimeToday ?: 0    
    def debugHours = debugRuntime / 3600
    def next = "Next event NULL"

    if (state.nextStartTime) {
    next = "Next Start:" + new Date(state.nextStartTime).format("h:mm a", location.timeZone)
    }
    if (state.stopTime) {
    next = "Next Stop:" + new Date(state.stopTime).format("h:mm a", location.timeZone)
    }
    

    logging("Pump - ${pump.displayName} turned OFF after ${actualRun/3600} Hrs Runtime:${String.format('%.3f', debugHours)} hrs Cycle#${state.cyclesToday} ${next}","info")
    if (state.totalRuntimeToday < dailyHours * 3600) {
    logging("Pump - ${pump.displayName} Scheduling cooldown. Runtime:${state.totalRuntimeToday} Target:${dailyHours * 3600}", "debug")
    state.nextStartTime = now() + (cooldownMinutes * 60 * 1000)
    logging("Pump - ${pump.displayName} Next start will be ${new Date(state.nextStartTime)}", "debug")
    runIn(cooldownMinutes * 60, startPumpCycle)
}
else {
    unschedule("monitorPump")
    logging("Pump - ${pump.displayName} Daily runtime complete", "info")
    
}
    logging("Pump - ${pump.displayName} After Stop: Runtime:${String.format('%.3f', debugHours)} hrs Cycle#${state.cyclesToday} ${next}", "debug")
}




// monitor run by cron when active

def monitorPump() {
def debugRuntime = state.totalRuntimeToday ?: 0
def today = new Date().format("yyyy-MM-dd", location.timeZone)
    if (state.lastMonitorRun && (now() - state.lastMonitorRun) < 60) {
    logging("Pump - ${pump.displayName} Monitor skipped - ran recently", "debug")
    return
}
if (state.schedulerPaused) { 
    logging("Pump - ${pump.displayName} Scheduler paused", "info")
    return
}

if (state.manualMode) {
    logging("Pump - ${pump.displayName} Manual mode active", "info")
    return
}       
if (state.lastRunDate != today) {
//    unschedule("monitorPump")
    logging("Pump - ${pump.displayName} Error New day not yet initialized  ", "info")
    logging("Pump - ${pump.displayName} lastRunDate=${state.lastRunDate} today=${today}", "debug")
    return
}    
 
    
state.lastMonitorRun = now()
    
if (state.isRunning && state.currentCycleStart) {
    debugRuntime += (now() - state.currentCycleStart) / 1000
}

    def debugHours = debugRuntime / 3600
    def next = "Next event NULL"

    if (state.nextStartTime) {
    next = "Next Start:" + new Date(state.nextStartTime).format("h:mm a", location.timeZone)
    }
    if (state.stopTime) {
    next = "Next Stop:" + new Date(state.stopTime).format("h:mm a", location.timeZone)
    }
 state.monitorCount = (state.monitorCount ?: 0) + 1 
// monitor now runs ever 30 not 10    
//    if(!debugLogging){
//    state.monitorCount = (state.monitorCount ?: 0) + 1   
//       if (state.monitorCount >= 3) {
    logging("Pump - ${pump.displayName} Monitor: Switch:${pump.currentSwitch} Runtime:${String.format('%.3f', debugHours)} hrs Cycle#${state.cyclesToday} ${next}","info") 
//    state.monitorCount =0
//        }    
//    }
//    else{
//    logging("Pump - ${pump.displayName} Monitor: Switch:${pump.currentSwitch} Runtime:${String.format('%.3f', debugHours)} hrs Cycle#${state.cyclesToday} ${next}","debug") 
//    }
    
    logging("Pump - ${pump.displayName} Monitor: Running:${state.isRunning} ${next}", "trace")

    
if (state.isRunning) {

    if (pump.currentSwitch != "on") {
        logging("Pump - ${pump.displayName} was OFF but schedule says ON - turning ON ${next}", "warn")
        pump.on()
    }

    if (!state.stopTime) {
        logging("Pump - ${pump.displayName} StopTime missing - rebuilding from current cycle", "warn")
        state.stopTime = state.currentCycleStart + (maxRunPerCycle * 3600 * 1000)
    }

    if (now() >= state.stopTime) {
        logging("Pump - ${pump.displayName} Fixing Stop timer expired ${Next}", "warn")
        stopPumpCycle()
    }


    if (state.nextStartTime) {
        logging("Pump - ${pump.displayName} Detected NextStart while running ${next}", "debug")
 //       state.nextStartTime = null  Just ignore. Its a debounce problem
    }



    } else {

        // Pump should be OFF
        if (pump.currentSwitch != "off") {
            logging("Pump - ${pump.displayName} was ON but schedule says OFF - turning OFF ${next}", "warn")
            pump.off()
        }

        // More runtime needed today?
        if (state.totalRuntimeToday < (dailyHours * 3600)) {

            // Lost cooldown timer?
            if (!state.nextStartTime) {
                logging("Pump - ${pump.displayName} Next Start missing - rebuilding cooldown", "warn")
                state.nextStartTime = now() + (cooldownMinutes * 60 * 1000)
            }

           
           // Is the cooldown finished?
 if (state.nextStartTime && now() >= state.nextStartTime) {
    logging("Pump - ${pump.displayName} cooldown expired - starting next cycle ${next}", "warn")
    startPumpCycle()
}                 
            

        } else {

            logging("Pump - ${pump.displayName} Daily runtime complete Runtime:${state.totalRuntimeToday} Cycles#${state.cyclesToday}", "info")
            unschedule("monitorPump")
            logging("Pump - ${pump.displayName} Monitor stopped", "info")


        }
    }
}
def footer() {
    section("Support Development") {
        paragraph """
<center>

Enjoying this app? Your support helps fund new features, bug fixes, and future Hubitat apps.<br>
<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif' width='160'></a>
<br>
<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'>Donate with PayPal</a><br>
<small>© 2026 TheMaster <a href='https://github.com/tmastersmart/hubitat-code/tree/main'>Smart Home Code</a> </small>
</center>
"""
    }
}
// ====================== LOGGING ROUTINE (App Version) ======================

def loggingUpdate() {
    log.info "Pump - ${pump.displayName} Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}] Locked:[${lockLogging}]"
 //lock code  
    

  
    if (!lockLogging){
     def timeDebug = 60
     def timeTrace = 60
     def DEBUG_LOG_TIMEOUT = 60 * timeDebug      
     def TRACE_LOG_TIMEOUT = 60 * timeTrace      
     if (debugLogging) {
         log.warn "Pump - ${pump.displayName} Debug logging will auto-disable in ${timeDebug} minutes"
        runIn(DEBUG_LOG_TIMEOUT, debugLogOff)
    }
     if (traceLogging) {
         log.warn "Pump - ${pump.displayName} Trace logging will auto-disable in ${timeTrace} minutes"
        runIn(TRACE_LOG_TIMEOUT, traceLogOff)
    }
  }     
}

void debugLogOff() {
    app.updateSetting("debugLogging", [value: "false", type: "bool"])
    log.debug "Pump - ${pump.displayName} Debug Logging Automatically Disabled"
}

void traceLogOff() {
    app.updateSetting("traceLogging", [value: "false", type: "bool"])
    log.trace "Pump - ${pump.displayName} Trace Logging Automatically Disabled"
}

private logging(String message, String level = "info") {
    switch(level) {
        case "error":
            log.error message
            break
        case "warn":
            log.warn message
            break
        case "trace":
            if (traceLogging) log.trace message
            break
        case "debug":
            if (debugLogging) log.debug message
            break
        case "info":
        default:
            if (infoLogging) log.info message
            break
    }
}
