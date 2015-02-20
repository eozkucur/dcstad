--
-- Created by IntelliJ IDEA.
-- User: ergin.ozkucur
-- Date: 20/02/15
-- Time: 21:16
-- To change this template use File | Settings | File Templates.
--

dcstad = {
    default_output_file = nil,
    aircraftstate=nil,
    socket=nil,
    mp=nil,
    tcpserver=nil,
    udpserver=nil,
    tcpclient=nil,
    timout=nil,
    isconnected=nil,
    clientip=nil,
    clientport=nil,
    prevLat=nil,
    prevLong=nil,
    lsf=nil,

    tadstart=function(self)
        self.aircraftstate={["posx"]=41.844450,["posy"]=41.955505,["bearing"]=0.2,["selectedwp"]=0,["waypoints"]={},["airobjects"]={}}
        self.timout=0.001
        self.isconnected=0

        self.lsf=require('lfs')
        self.default_output_file = io.open(self.lfs.writedir().."/Logs/Export.log", "w")

        package.path  = package.path..";"..self.lfs.currentdir().."/LuaSocket/?.lua"
        package.cpath = package.cpath..";"..self.lfs.currentdir().."/LuaSocket/?.dll"
        package.path  = package.path..";"..self.lfs.writedir().."/Scripts/MessagePack/?.lua"
        self.socket = require("socket")
        self.mp = require("MessagePack")
        --self.default_output_file:write(string.format("Start\n"))
        self.tcpserver=self.socket.bind("*", 5556)
        self.tcpserver:settimeout(self.timout)
        self.udpserver = self.socket.udp()
    end,
    tadstop=function(self)
        if self.default_output_file then
            self.default_output_file:close()
            self.default_output_file = nil
        end
        if self.tcpclient then
            self.socket.try(self.tcpclient:close())
        end
        self.socket.try(self.tcpserver:close())
        self.socket.try(self.udpserver:close())
    end,


    tablecount=function(self,tbl)
        local c=0
        for k in pairs(tbl) do c=c+1 end
        return c
    end,

    table_print=function(self,tt, indent, done)
        done = done or {}
        indent = indent or 0
        if type(tt) == "table" then
            local sb = {}
            for key, value in pairs (tt) do
                table.insert(sb, string.rep (" ", indent)) -- indent it
                if type (value) == "table" and not done [value] then
                    done [value] = true
                    if "number" == type(key) then
                        table.insert(sb, string.format("%d = {\n",tostring(key)));
                    else
                        table.insert(sb, string.format("%s = {\n",tostring(key)));
                    end
                    --table.insert(sb, "{\n");
                    table.insert(sb, table_print (value, indent + 2, done))
                    table.insert(sb, string.rep (" ", indent)) -- indent it
                    table.insert(sb, "}\n");
                elseif "number" == type(key) then
                    table.insert(sb, string.format("%d = \"%s\"\n",tostring (key), tostring(value)))
                else
                    table.insert(sb, string.format(
                        "%s = \"%s\"\n", tostring (key), tostring(value)))
                end
            end
            return table.concat(sb)
        else
            return tt .. "\n"
        end
    end,

    table_to_string=function( self,tbl )
        if  "nil"       == type( tbl ) then
            return tostring(nil)
        elseif  "table" == type( tbl ) then
            return table_print(tbl)
        elseif  "string" == type( tbl ) then
            return tbl
        else
            return tostring(tbl)
        end
    end,

    readAndSendData=function(self)
        if self.isconnected==1 then
            local line,error=self.tcpclient:receive()
            if error=="closed" then
                --self.default_output_file:write("closed\n")
                self.socket.try(self.tcpclient:close())
                self.isconnected=0
            else
                local selfdata=LoGetSelfData()

                if selfdata then
                    self.aircraftstate["posy"]=selfdata.LatLongAlt.Lat;
                    self.aircraftstate["posx"]=selfdata.LatLongAlt.Long;
                    self.aircraftstate["bearing"]=selfdata.Heading;

                    self.default_output_file:write("objects:\n")
                    self.default_output_file:write(string.format("%s",self:table_to_string(LoGetWorldObjects())))
                    self.default_output_file:write("wings:\n")
                    self.default_output_file:write(string.format("%s",self:table_to_string(LoGetWingInfo())))
                    self.aircraftstate["airobjects"]={}
                    local allobjects=LoGetWorldObjects()
                    for k,v in pairs(allobjects) do
                        if (v.Type.level1==1 and (v.Type.level2==1 or v.Type.level2==2) and v.CoalitionID==selfdata.CoalitionID and k~=LoGetPlayerPlaneId()) then
                            local ao={}
                            ao["posy"]=v.LatLongAlt.Lat
                            ao["posx"]=v.LatLongAlt.Long
                            ao["bearing"]=v.Heading
                            ao["groupid"]=1
                            self.aircraftstate["airobjects"][k]=ao
                        end
                    end
                    local wings=LoGetWingInfo()
                    for k,v in pairs(wings) do
                        local ao=self.aircraftstate["airobjects"][tonumber(v.wingmen_id)]
                        if ao then
                            ao.groupid=0
                        end
                    end
                    self.default_output_file:write("refined:\n")
                    self.default_output_file:write(string.format("%s",self:table_to_string(self.aircraftstate)))
                end
                local route = LoGetRoute()
                if route then
                    local latlong = LoLoCoordinatesToGeoCoordinates(route.goto_point.world_point.x,route.goto_point.world_point.z)
                    local wp={}
                    wp["posy"]=latlong.latitude
                    wp["posx"]=latlong.longitude
                    wp["id"]=route.goto_point.this_point_num-1
                    self.aircraftstate["waypoints"][wp["id"]]=wp
                    self.aircraftstate["selectedwp"]=wp["id"]
                end
                self.default_output_file:write(string.format("sending %f %f %f\n",self.aircraftstate['posx'],self.aircraftstate['posy'],self.aircraftstate['bearing']))
                local buffer={}
                self.mp.packers['float'](buffer,self.aircraftstate['posx'])
                self.mp.packers['float'](buffer,self.aircraftstate['posy'])
                self.mp.packers['float'](buffer,self.aircraftstate['bearing'])
                self.mp.packers['signed'](buffer,self:tablecount(self.aircraftstate['waypoints']))
                self.default_output_file:write(string.format("wpsize %d\n",self:tablecount(self.aircraftstate['waypoints'])))
                for _,v in pairs(self.aircraftstate['waypoints']) do
                    self.default_output_file:write(string.format("wp: %f %f \n",v['posx'],v['posy']))
                    self.mp.packers['float'](buffer,v['posx'])
                    self.mp.packers['float'](buffer,v['posy'])
                    self.mp.packers['signed'](buffer,v['id'])
                end
                self.mp.packers['signed'](buffer,self.aircraftstate['selectedwp'])

                self.mp.packers['signed'](buffer,self:tablecount(self.aircraftstate['airobjects']))
                for _,v in pairs(self.aircraftstate['airobjects']) do
                    self.default_output_file:write(string.format("wp: %f %f \n",v['posx'],v['posy']))
                    self.mp.packers['float'](buffer,v['posx'])
                    self.mp.packers['float'](buffer,v['posy'])
                    self.mp.packers['float'](buffer,v['bearing'])
                    self.mp.packers['signed'](buffer,v['groupid'])
                end
                self.udpserver:sendto(table.concat(buffer), self.clientip, 5555)
            end
        else
            local readable, _, error = self.socket.select({self.tcpserver}, nil,self.timout)
            if readable[1] then
                if self.isconnected==0 then
                    self.tcpclient=self.tcpserver:accept()
                    if self.tcpclient then
                        self.tcpclient:settimeout(self.timout)
                        self.clientip,self.clientport=self.tcpclient:getpeername()
                        self.isconnected=1
                    end
                else

                end
            end
        end
    end,


    tadupdate=function(self)
        local status,err = pcall(self.readAndSendData)

        if not status then
            --self.default_output_file:write("err\n")
        end
    end
}


do
    local starttmp=LuaExportStart;
    LuaExportStart=function()
        dcstad:tadstart()
        if starttmp then
            starttmp()
        end
    end

    local updatetmp=LuaExportActivityNextEvent;
    LuaExportActivityNextEvent=function(t)
        dcstad:tadupdate()
        if updatetmp then
            return updatetmp(t);
        else
            return t+0.25
        end
    end

    local stoptmp=LuaExportStop;
    LuaExportStop=function()
        dcstad:tadstop()
        if stoptmp then
            stoptmp();
        end
    end
end
