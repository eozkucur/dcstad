--
-- Created by IntelliJ IDEA.
-- User: ergin.ozkucur
-- Date: 15/02/15
-- Time: 20:15
-- To change this template use File | Settings | File Templates.
--
local socket=require("socket")
local mp = require("MessagePack")
print("Start...")
local tcpserver
local udpserver
local tcpclient
local timout=0.001
local isconnected=0
local clientip
local clientport

local aircraftstate={["posx"]=41.844450,["posy"]=41.955505,["bearing"]=15,["waypoints"]={
    {["id"]=1,["posx"]=41.777159,["posy"]=41.986136},
    {["id"]=2,["posx"]=41.895949,["posy"]=41.899566},
    {["id"]=3,["posx"]=41.991049,["posy"]=41.976693},
    {["id"]=4,["posx"]=42.098166,["posy"]=41.933544}}}

function init()
    print("init")
    tcpserver=socket.bind("*", 5556)
    tcpserver:settimeout(timout)
    udpserver = socket.udp()
end

function update()
    print("update")
    if isconnected==1 then
        local line,error=tcpclient:receive()
        if error=="closed" then
            print("closed")
            socket.try(tcpclient:close())
            isconnected=0
        else
            print("sending")
            local buffer={}
            mp.packers['double'](buffer,aircraftstate['posx'])
            mp.packers['double'](buffer,aircraftstate['posy'])
            mp.packers['double'](buffer,aircraftstate['bearing'])
            mp.packers['signed'](buffer,#aircraftstate['waypoints'])
            --mp.packers['signed'](buffer,table.getn(aircraftstate['waypoints']))
            for _,v in ipairs(aircraftstate['waypoints']) do
                mp.packers['double'](buffer,v['posx'])
                mp.packers['double'](buffer,v['posy'])
                mp.packers['signed'](buffer,v['id'])
            end
            --bufferraw=table.tconcat(buffer)
            udpserver:sendto(table.concat(buffer), clientip, 5555)
        end
    else
        local readable, _, error = socket.select({tcpserver}, nil,timout)
        if readable[1] then
            print("have something")
            if isconnected==0 then
                tcpclient=tcpserver:accept()
                if tcpclient then
                    tcpclient:settimeout(timout)
                    clientip,clientport=tcpclient:getpeername()
                    isconnected=1
                end
            else

            end
        else
            print("nothing")
        end
    end


    --tcpserver:settimeout(0)
    --tcpclient=server:accept()
    --
    --local line, err = client:receive()
    --if not err then
    --    client:send(line .. "\n")
    --end
end

--local mp = require "MessagePack"
--mpac = mp.pack(data)
--data = mp.unpack(mpac)

function sleep(sec)
    socket.select(nil, nil, sec)
end

init()

sleep(0.1)
for i=1,10000 do
    update()
    sleep(0.1)
end

--packer.write(state.pos.x);
--packer.write(state.pos.y);
--packer.write(state.bearing);
--packer.write(state.waypoints.size());
--for(int i=0;i<state.waypoints.size();i++){
--    wp=state.waypoints.get(i);
--    packer.write(wp.pos.x);
--    packer.write(wp.pos.y);
--    packer.write(wp.id);
--}
--byte[] buf=bout.toByteArray();

print("Finish...")