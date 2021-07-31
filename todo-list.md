# 规划清单


## CLI指令

elapse dump packageName -o output.txt   dump当前timeLine

elapse slow packageName    连续输出卡顿日志
   = tail elapse-slow-xxxx-xxxx-pidxxxx.txt

elapse set --slowThreshold  300
elapse set --recordDuration 300
elapse set --debuggable true|false
elapse set --log-level all|slow-only

elapse -c 清除elapse dump/slow文件


