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

    tadstart=function()
        aircraftstate={["posx"]=41.844450,["posy"]=41.955505,["bearing"]=0.2,["selectedwp"]=0,["waypoints"]={},["airobjects"]={}}
        timout=0.001
        isconnected=0

        --default_output_file = io.open(lfs.writedir().."/Logs/Export.log", "w")

        package.path  = package.path..";"..lfs.currentdir().."/LuaSocket/?.lua"
        package.cpath = package.cpath..";"..lfs.currentdir().."/LuaSocket/?.dll"
        package.path  = package.path..";"..lfs.writedir().."/Scripts/MessagePack/?.lua"
        socket = require("socket")
        mp = require("MessagePack")
        --default_output_file:write(string.format("Start\n"))
        tcpserver=socket.bind("*", 5556)
        tcpserver:settimeout(timout)
        udpserver = socket.udp()
    end,
    tadstop=function()
        if default_output_file then
            default_output_file:close()
            default_output_file = nil
        end
        if tcpclient then
            socket.try(tcpclient:close())
        end
        socket.try(tcpserver:close())
        socket.try(udpserver:close())
    end,


    tablecount=function(tbl)
        local c=0
        for k in pairs(tbl) do c=c+1 end
        return c
    end,

    table_print=function(tt, indent, done)
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

    table_to_string=function( tbl )
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

    readAndSendData=function()
        if isconnected==1 then
            local line,error=tcpclient:receive()
            if error=="closed" then
                --default_output_file:write("closed\n")
                socket.try(tcpclient:close())
                isconnected=0
            else
                local selfdata=LoGetSelfData()

                if selfdata then
                    prevLat=selfdata.LatLongAlt.Lat
                    prevLong=selfdata.LatLongAlt.Long
                    aircraftstate["posy"]=selfdata.LatLongAlt.Lat;
                    aircraftstate["posx"]=selfdata.LatLongAlt.Long;
                    aircraftstate["bearing"]=selfdata.Heading;

                    --default_output_file:write("objects:\n")
                    --default_output_file:write(string.format("%s",table_to_string(LoGetWorldObjects())))
                    --default_output_file:write("wings:\n")
                    --default_output_file:write(string.format("%s",table_to_string(LoGetWingInfo())))
                    aircraftstate["airobjects"]={}
                    local allobjects=LoGetWorldObjects()
                    for k,v in pairs(allobjects) do
                        if (v.Type.level1==1 and (v.Type.level2==1 or v.Type.level2==2) and v.CoalitionID==selfdata.CoalitionID and k~=LoGetPlayerPlaneId()) then
                            local ao={}
                            ao["posy"]=v.LatLongAlt.Lat
                            ao["posx"]=v.LatLongAlt.Long
                            ao["bearing"]=v.Heading
                            ao["groupid"]=1
                            aircraftstate["airobjects"][k]=ao
                        end
                    end
                    local wings=LoGetWingInfo()
                    for k,v in pairs(wings) do
                        local ao=aircraftstate["airobjects"][tonumber(v.wingmen_id)]
                        if ao then
                            ao.groupid=0
                        end
                    end
                    --default_output_file:write("refined:\n")
                    --default_output_file:write(string.format("%s",table_to_string(aircraftstate)))
                end
                local route = LoGetRoute()
                if route then
                    local latlong = LoLoCoordinatesToGeoCoordinates(route.goto_point.world_point.x,route.goto_point.world_point.z)
                    local wp={}
                    wp["posy"]=latlong.latitude
                    wp["posx"]=latlong.longitude
                    wp["id"]=route.goto_point.this_point_num-1
                    aircraftstate["waypoints"][wp["id"]]=wp
                    aircraftstate["selectedwp"]=wp["id"]
                end
                --default_output_file:write(string.format("sending %f %f %f\n",aircraftstate['posx'],aircraftstate['posy'],aircraftstate['bearing']))
                local buffer={}
                mp.packers['float'](buffer,aircraftstate['posx'])
                mp.packers['float'](buffer,aircraftstate['posy'])
                mp.packers['float'](buffer,aircraftstate['bearing'])
                mp.packers['signed'](buffer,tablecount(aircraftstate['waypoints']))
                --default_output_file:write(string.format("wpsize %d\n",tablecount(aircraftstate['waypoints'])))
                for _,v in pairs(aircraftstate['waypoints']) do
                    --default_output_file:write(string.format("wp: %f %f \n",v['posx'],v['posy']))
                    mp.packers['float'](buffer,v['posx'])
                    mp.packers['float'](buffer,v['posy'])
                    mp.packers['signed'](buffer,v['id'])
                end
                mp.packers['signed'](buffer,aircraftstate['selectedwp'])

                mp.packers['signed'](buffer,tablecount(aircraftstate['airobjects']))
                for _,v in pairs(aircraftstate['airobjects']) do
                    --default_output_file:write(string.format("wp: %f %f \n",v['posx'],v['posy']))
                    mp.packers['float'](buffer,v['posx'])
                    mp.packers['float'](buffer,v['posy'])
                    mp.packers['float'](buffer,v['bearing'])
                    mp.packers['signed'](buffer,v['groupid'])
                end
                udpserver:sendto(table.concat(buffer), clientip, 5555)
            end
        else
            local readable, _, error = socket.select({tcpserver}, nil,timout)
            if readable[1] then
                if isconnected==0 then
                    tcpclient=tcpserver:accept()
                    if tcpclient then
                        tcpclient:settimeout(timout)
                        clientip,clientport=tcpclient:getpeername()
                        isconnected=1
                    end
                else

                end
            end
        end
    end,


    tadupdate=function()
        local status,err = pcall(ReadAndSendData)

        if not status then
            --default_output_file:write("err\n")
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
