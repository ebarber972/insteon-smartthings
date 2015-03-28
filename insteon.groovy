/**
 *  Insteon Switch (LOCAL)
 *
 *  Copyright 2014 patrick@patrickstuart.com
 *  Updated 1/4/15 by goldmichael@gmail.com
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
  definition (name: "Insteon Switch (LOCAL)", namespace: "michaelgold", author: "patrick@patrickstuart.com/tslagle13@gmail.com/goldmichael@gmail.com") {
    capability "Switch Level"
    capability "Actuator"
    capability "Indicator"
    capability "Switch"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    
  }
  
  preferences {
    input("InsteonIP", "string", title:"Insteon IP Address", description: "Please enter your Insteon Hub IP Address", defaultValue: "192.168.1.2", required: true, displayDuringSetup: true)
    input("InsteonPort", "string", title:"Insteon Port", description: "Please enter your Insteon Hub Port", defaultValue: 25105, required: true, displayDuringSetup: true)
    input("InsteonID", "string", title:"Device Insteon ID", description: "Please enter the devices Insteon ID - numbers only", defaultValue: "1E65F2", required: true, displayDuringSetup: true)
    input("InsteonHubUsername", "string", title:"Insteon Hub Username", description: "Please enter your Insteon Hub Username", defaultValue: "michael" , required: true, displayDuringSetup: true)
    input("InsteonHubPassword", "password", title:"Insteon Hub Password", description: "Please enter your Insteon Hub Password", defaultValue: "password" , required: true, displayDuringSetup: true)
  }
  
  simulator {
    // status messages
    status "on": "on/off: 1"
    status "off": "on/off: 0"
    
    // reply messages
    reply "zcl on-off on": "on/off: 1"
    reply "zcl on-off off": "on/off: 0"
  }
  
  // UI tile definitions
  tiles {
    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
      state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
      state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      state "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
      state "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
    }
    standardTile("indicator", "device.indicatorStatus", inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
      state "level", action:"switch level.setLevel"
    }
    
    main(["switch"])
    details(["switch", "refresh", "indicator", "levelSliderControl"])
  }
}

// handle commands
def on() {
  //log.debug "Executing 'take'"
  sendEvent(name: "switch", value: "on")
  sendEvent(name: "level", value: 75)
  sendCmd(11,"C0")
}

def off() {
  log.debug("off")
  sendEvent(name: "switch", value: "off")
  sendEvent(name: "level", value: 0)
  sendCmd(13,"00")
}


private String convertIPtoHex(ipAddress) { 
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
  return hex

}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() )
  return hexport
}


def localHttp(path){
  log.debug "path is: $path"
  
  def hosthex = convertIPtoHex(InsteonIP)
  def porthex = convertPortToHex(InsteonPort)
  /*device.deviceNetworkId = "$hosthex:$porthex:${InsteonID}" 
  log.debug device.deviceNetworkId
  */
  def userpassascii = "${InsteonHubUsername}:${InsteonHubPassword}"
  def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
  def headers = [:] //"HOST:" 
  headers.put("HOST", "$InsteonIP:$InsteonPort")
  headers.put("Authorization", userpass)
  
  try {
    def hubAction = new physicalgraph.device.HubAction(
      method:  "GET",
      path: path,
      headers: headers
    )
    log.debug hubAction
    hubAction
  }
  catch (Exception e) {
    log.debug "Hit Exception on $hubAction"
    log.debug e
  }
}

def sendCmd(cmd, level){
    def host = InsteonIP
    def path = "/3?0262" + "${InsteonID}"  + "0F" + "${cmd}" + "${level}" + "=I=3"
    return localHttp(path)
}

def setLevel(value) {
  log.debug "setting level ${value}"
  
  def level = 255*(Math.min(value as Integer, 99))/100
  level = Integer.toHexString(level as Integer )

  log.debug "setting level ${level}"

  if(value == 0){
    return off()
  }
  sendEvent(name: "switch", value: "on")

  if(level.size() == 1){
    sendCmd(11, "0${level}")
  }else{
    sendCmd(11, "${level}")
  }
}
def setLevel(value, duration) {
  log.debug "setting level ${value} ${duration}"
}

def refresh(){
  localHttp("/1?XB=M=1")
  def y = 1000
  while ( y-- > 0 ) {
  }
  sendCmd(19, "00")
  y = 1000
  while ( y-- > 0 ) {
  }
  
  
  localHttp("/buffstatus.xml")
  
  
}

def poll(){
  refresh()
}

def getBufferStatus(){
  localHttp("/buffstatus.xml")
  
}
def parseDescriptionAsMap(description) {
  description.split(",").inject([:]) { map, param ->
    def nameAndValue = param.split(":")
    if (nameAndValue.size() == 2){
      map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
  }
}


def parse(String description) {
  def map = [:]
  
  def descMap = parseDescriptionAsMap(description)
  
  if(descMap && descMap["body"]) {
    def content = new String(descMap.body.decodeBase64())
    
      def l =   Integer.parseInt(content[52..53],16)
      def level = (l * 100/255) as Integer
      
      log.debug("current level: ${level}")
      if (l == 0){
	sendEvent(name: "switch", value: "off")
      }else{
	sendEvent(name: "switch", value: "on")
      }
      sendEvent(name: "level", value: level);
      
    
  }
}
