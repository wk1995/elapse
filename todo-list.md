# 规划清单


## CLI指令

raster dump packageName -o output.txt   dump当前timeLine

raster slow packageName    连续输出卡顿日志
   = tail raster-slow-xxxx-xxxx-pidxxxx.txt

raster set --slowThreshold  300
raster set --recordDuration 300
raster set --debuggable true|false
raster set --log-level all|slow-only

raster -c 清除raster dump/slow文件


