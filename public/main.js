// 最初の読み込み
loadBenchmarks();
updatePCInfo();

// 1秒ごとに再取得
let updateInterval = 1000;
setInterval(updatePCInfo, updateInterval);

let DynamicInfo = {};
let StaticInfo = {};
let totalMemory = {};
let timeStaticPCInfoChanged = 0;
let serverIP;

async function updatePCInfo() {
  const dRes = await fetch(`/dynamic?ts=${Date.now()}`);
  const dynamicData = await dRes.json();
  if (dynamicData.timeStaticPCInfoChanged != timeStaticPCInfoChanged) {
    timeStaticPCInfoChanged = dynamicData.timeStaticPCInfoChanged;
    const sRes = await fetch(`/static?ts=${Date.now()}`);
    const staticData = await sRes.json();
    renderStaticHTML(staticData);
    console.log("HTML形成");

    updateDynamicData(dynamicData);
    return;
  }

  console.log("動的情報更新");
  updateDynamicData(dynamicData);
}

function renderStaticHTML(staticData) {
  // ---- コンテナ取得＆初期化 ----
  getElementConfirmlyByIdAndInitialize("dashboard-load");
  getElementConfirmlyByIdAndInitialize("dashboard-cpu");
  getElementConfirmlyByIdAndInitialize("dashboard-memory");
  getElementConfirmlyByIdAndInitialize("dashboard-gpu");
  getElementConfirmlyByIdAndInitialize("dashboard-network");
  serverIP = staticData.serverIPAddress;
  renderServerIPAddress(staticData.serverIPAddress);
  let ipAddresses = structuredClone(staticData.allIPAddresses);

  // ---- IP ごとにカードを描画 ----
  (staticData.allIPAddresses || []).forEach(ip => {
    const staticPCInfo = (staticData.staticPCInfos && staticData.staticPCInfos[ip]) || {};

    // 総メモリを保存（後で使用）
    totalMemory[ip] = staticPCInfo.TotalMemory ?? 0;

    // GPUs: Map/Object → Array に正規化（Name が無ければキー名を使う）
    const gpusArr = Array.isArray(staticPCInfo.GPUs)
      ? staticPCInfo.GPUs
      : Object.entries(staticPCInfo.GPUs || {}).map(([name, gpu]) => ({
          ...(gpu || {}),
          Name: (gpu && gpu.Name) ? gpu.Name : name
        }));

    // NetworkCards: Map/Object → Array に正規化
    const networksArr = Array.isArray(staticPCInfo.NetworkCards)
      ? staticPCInfo.NetworkCards
      : Object.entries(staticPCInfo.NetworkCards || {}).map(([name, nc]) => ({
          Name: name,
          DisplayName: (nc && nc.DisplayName) ? nc.DisplayName : name,
          Bandwidth: (nc && typeof nc.Bandwidth === "number") ? nc.Bandwidth : 0
        }));

    // CPU オブジェクト補完
    const cpuStatic = staticPCInfo.CPU || { Name: "Unknown", ClockSpeed: 0 };

    // IPごとのCPUベンチスコアを保持（Unknownは1にしておく）
    const score = findCPUBenchmark(cpuStatic.Name);
    cpuScoreByIp[ip] = Number.isFinite(Number(score)) ? Number(score) : 1;

    // 各カード描画
    renderLoadAverageCard(ip);
    renderCPUCard(ip, cpuStatic);
    renderMemoryCard(ip, staticPCInfo.TotalMemory ?? 0);
    renderGPUCard(ip, gpusArr);
    renderNetworkCard(ip, networksArr);

    // ネットワークスピード表のヘッダー側カード
    ipAddresses.splice(0, 1);
    renderNetworkSpeedCard(ip, ipAddresses);
  });
}

function getElementConfirmlyById(Id) {
  const container = document.getElementById(Id);
  if (!container) {
    console.log("couldn't find Id(" + Id + ").")
    return;
  } else {
    return container;
  }
}

function getElementConfirmlyByIdAndInitialize(Id) {
  const container = document.getElementById(Id);
  if (container) {
    container.innerHTML = "";
  } else {
    console.log("couldn't find Id(" + Id + ").");
  }
}

function renderServerIPAddress(serverIPAddress) {
  const containerServerIP = getElementConfirmlyById("server-ip");
  containerServerIP.innerText = serverIPAddress;
}

function renderCard(Id, innerHTML) {
  const container = getElementConfirmlyById(Id);
  const card = document.createElement("div");
  card.className = "card";
  card.innerHTML = innerHTML;
  container.appendChild(card);
}

function renderTimeLine(id, innerHTML) {
  const container = getElementConfirmlyById(id);
  container.innerHTML += innerHTML;
}

function renderLoadAverageCard(ip) {
  const loadInnerHTML =
    `
      <div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
      <div style="margin-top: 0.5em;"><strong>IP:</strong> ${ip}</div>
      <div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">Load Average</div>
      <div class="LoadAverage" data-ip="${ip}" style="font-size: 24px; margin-top: 0.3em; color: #ff9800; font-weight: bold;"></div>
    `;
  renderCard("dashboard-load", loadInnerHTML);
  const timeLineInnerHTML =
    `
      <div style="margin-top:0.4em;"><strong>LoadAverage: </strong>
        <span class="loadAveNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>LoadAverage Timeline</strong></div>
      <div class="chart-container">
        <canvas class="loadAveLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
    `;

  renderTimeLine("LoadAverageTimeLine", timeLineInnerHTML);
}

// --- クロック・CPU使用率カード ---
function renderCPUCard(ip, CPU) {
  const cpuBenchmarkScore = findCPUBenchmark(CPU.Name);
  const cpuInnerHTML =
    `
      <div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
      <div><strong>IP:</strong> ${ip}</div>
      <div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">${CPU.Name}</div>
      <div><strong>Clock Speed:</strong><p class="cpuClockSpeed" data-ip="${ip}"></p></div>
      <div><strong>Benchmark:</strong> ${cpuBenchmarkScore}</div>
      <div style="margin-top: 0.8em;"><strong>CPU使用率</strong></div>
      <div class="chart-container">
        <canvas class="cpuChart canvas" data-ip="${ip}"></canvas>
      </div>
      <div style="margin-top:0.4em;"><strong>CPU Perf(= % × Score): </strong>
         <span class="cpuPerfNow" data-ip="${ip}">-</span>
       </div>
    `;
  renderCard("dashboard-cpu", cpuInnerHTML);

  const cpuTimeLineInnerHTML =
    `
      <div style="margin-top:0.4em;"><strong>CPU Performance: </strong>
        <span class="cpuPerfNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>CPU Performance Timeline</strong></div>
      <div class="chart-container">
        <canvas class="cpuPerfLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
            
      <div style="margin-top:0.4em;"><strong>CPU Process Load: </strong>
        <span class="cpuLoadNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>CPU Process Load Timeline</strong></div>
      <div class="chart-container">
        <canvas class="cpuLoadLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
      
      <div style="margin-top:0.4em;"><strong>CPU JVM User: </strong>
        <span class="cpuJVMUserNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>CPU JVM User Timeline</strong></div>
      <div class="chart-container">
        <canvas class="cpuJVMUserLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
      
      <div style="margin-top:0.4em;"><strong>CPU JVM System: </strong>
        <span class="cpuJVMSysNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>CPU JVM System Timeline</strong></div>
      <div class="chart-container">
        <canvas class="cpuJVMSysLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
            
      <div style="margin-top:0.4em;"><strong>CPU JVM Total: </strong>
        <span class="cpuJVMNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>CPU JVM Total Timeline</strong></div>
      <div class="chart-container">
        <canvas class="cpuJVMLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
    `;
  renderTimeLine("CPUTimeLine", cpuTimeLineInnerHTML);
}

function renderMemoryCard(ip, totalMemoryVal) {
  const memoryInnerHTML =
    `
      <div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
      <div><strong>IP:</strong> ${ip}</div>
      <div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">Memory</div>
      <div><strong>Used Memory:</strong><p class="usedMemory" data-ip="${ip}">${totalMemoryVal}</p></div>
      <div style="margin-top: 0.8em;"><strong>Memory Usage</strong></div>
      <div class="chart-container">
        <canvas class="memoryChart canvas" data-ip="${ip}"></canvas>
      </div>
    `;
  renderCard("dashboard-memory", memoryInnerHTML);

  const memoryTimeLineHTML =
    `
      <div style="margin-top:0.4em;"><strong>Real Memory: </strong>
        <span class="memRealNow" data-ip="${ip}">-</span> GB
      </div>
      <div style="margin-top:0.6em;"><strong>Real Memory Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memRealLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
            
      <div style="margin-top:0.4em;"><strong>Swap Memory: </strong>
        <span class="memSwapNow" data-ip="${ip}">-</span> GB
      </div>
      <div style="margin-top:0.6em;"><strong>Swap Memory Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memSwapLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
            
      <div style="margin-top:0.4em;"><strong>Heap Memory: </strong>
        <span class="memHeapNow" data-ip="${ip}">-</span> GB
      </div>
      <div style="margin-top:0.6em;"><strong>Heap Memory Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memHeapLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
            
      <div style="margin-top:0.4em;"><strong>Non Heap Memory: </strong>
        <span class="memNonHeapNow" data-ip="${ip}">-</span> GB
      </div>
      <div style="margin-top:0.6em;"><strong>Non Heap Memory Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memNonHeapLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
      
      <div style="margin-top:0.4em;"><strong>GC: </strong>
        <span class="memGCYoungNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>GC Young Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memGCYoungLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
      
      <div style="margin-top:0.4em;"><strong>GC: </strong>
        <span class="memGCOldNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>GC Old Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memGCOldLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
      
      <div style="margin-top:0.4em;"><strong>GC JFR: </strong>
        <span class="memGCJFRNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>GC JFR Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memGCJFRLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
      
      <div style="margin-top:0.4em;"><strong>GC JFR Pause Millis: </strong>
        <span class="memGCJFRPauseNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong>GC JFR Pause Millis Timeline</strong></div>
      <div class="chart-container">
        <canvas class="memGCJFRPauseLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
    `;
  renderTimeLine("MemoryTimeLine", memoryTimeLineHTML);
}

// --- GPUカード ---
function renderGPUCard(ip, GPUs) {
  let gpuList = "";
  let gpuTimeLines = "";

  if (GPUs && Array.isArray(GPUs)) {
    GPUs.forEach(gpu => {
      const gpuBenchmarkScore = findGPUBenchmark(gpu.Name);
      const vram = (gpu.VRam / 1_000_000_000).toFixed(2) || 0;
      if (gpuBenchmarkScore != "Unknown") {
        gpuList +=
          `
            <div style="margin-top: 0.5em; font-weight: bold; background: #1c4793; color: white;">${gpu.Name}</div>
            <div class="gpu-info-row">
              <div class="gpu-labels">
                <div>Load:</div>
                <div>VRAM:</div>
                <div>Temp:</div>
                <div>Benchmark:</div>
              </div>
              <div class="gpu-values">
                <div> %</div>
                <div class="gpuMemory" data-ip="${ip}" data-gpu="${gpu.Name}">${vram}GB</div>
                <div class="gpuTemperature" data-ip="${ip}" data-gpu="${gpu.Name}">Unknown</div>
                <div>${gpuBenchmarkScore}</div>
              </div>
            </div>
            <div class="gpu-chart">
              <canvas class="gpuChart canvas" data-ip="${ip}" data-gpu="${gpu.Name}"></canvas>
            </div>
          `;
        gpuTimeLines +=
          `   
            <div style="margin-top:0.4em;"><strong>${gpu.Name} Performance</strong>
              <span class="gpuPerfNow" data-ip="${ip}" data-gpu="${gpu.Name}">-</span>
            </div>
            <div style="margin-top:0.6em;"><strong>${gpu.Name}</strong></div>
            <div class="chart-container" style="margin-top:0.5em;">
              <canvas class="gpuPerfLine canvas" data-ip="${ip}" data-gpu="${gpu.Name}" width="900" height="300"></canvas>
            </div>
            
            <div style="margin-top:0.4em;"><strong>${gpu.Name} Temperature</strong>
              <span class="gpuTempNow" data-ip="${ip}" data-gpu="${gpu.Name}">-</span>
            </div>
            <div style="margin-top:0.6em;"><strong>${gpu.Name}</strong></div>
            <div class="chart-container" style="margin-top:0.5em;">
              <canvas class="gpuTempLine canvas" data-ip="${ip}" data-gpu="${gpu.Name}" width="900" height="300"></canvas>
            </div>
            
            <div style="margin-top:0.4em;"><strong>${gpu.Name} Memory</strong>
              <span class="gpuMemNow" data-ip="${ip}" data-gpu="${gpu.Name}">-</span>
            </div>
            <div style="margin-top:0.6em;"><strong>${gpu.Name}</strong></div>
            <div class="chart-container" style="margin-top:0.5em;">
              <canvas class="gpuMemLine canvas" data-ip="${ip}" data-gpu="${gpu.Name}" width="900" height="300"></canvas>
            </div>
          `;

        renderTimeLine("GPUTimeLine", gpuTimeLines);
      }
      const score = findCPUBenchmark(gpu.Name);
      gpuScoreByKey[ip + gpu.Name] = Number.isFinite(Number(score)) ? Number(score) : 1;
    });
  }

  const gpuInnerHTML =
    `
      <div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
      <div><strong>IP:</strong> ${ip}</div>
      <div style="margin: 0.5em 0; font-weight: bold; background: #1c4793; color: white;">GPU</div>
      ${gpuList}
    `;
  renderCard("dashboard-gpu", gpuInnerHTML);
}

// --- ネットワーク ---
function renderNetworkCard(ip, Networks) {
  let networkRows = "";
  let allBandwidth = 0;

  if (Networks && Array.isArray(Networks)) {
    Networks.forEach(networkCard => {
      const display = networkCard.DisplayName || networkCard.Name;
      const rawBandwidth = networkCard.Bandwidth || 0;
      const bandwidthGbps = (rawBandwidth / 1_000_000_000).toFixed(2);
      const bandwidthMbps = (rawBandwidth / 1_000_000);
      allBandwidth += rawBandwidth / 1_000_000;

      if (bandwidthMbps == 0) return;

      const displayBandwidth = rawBandwidth === 0
        ? "<span style='color:gray;'>Unknown</span>"
        : bandwidthGbps + ` Gbps`;

      networkRows +=
        `
          <tr>
            <td>${display}</td>
            <td class="upload" data-ip="${ip}" data-NC="${networkCard.Name}"></td>
            <td class="download" data-ip="${ip}" data-NC="${networkCard.Name}"></td>
          </tr>
        `;

      const networkTimeLineInnerHTML =
        `
          <div style="margin-top:0.4em;"><strong>${networkCard.Name}Upload Speed:</strong>
            <span class="netUpNow" data-ip="${ip}" data-net="${networkCard.Name}">-</span>
          </div>
          <div style="margin-top:0.6em;"><strong>${networkCard.Name}</strong></div>
          <div class="chart-container" style="margin-top:0.5em;">
            <canvas class="netUpLine canvas" data-ip="${ip}" data-net="${networkCard.Name}" width="900" height="300"></canvas>
          </div>
                              
          <div style="margin-top:0.4em;"><strong>${networkCard.Name} Download Speed:</strong>
            <span class="netDownNow" data-ip="${ip}" data-net="${networkCard.Name}">-</span>
          </div>
          <div style="margin-top:0.6em;"><strong>${networkCard.Name}</strong></div>
          <div class="chart-container" style="margin-top:0.5em;">
            <canvas class="netDownLine canvas" data-ip="${ip}" data-net="${networkCard.Name}" width="900" height="300"></canvas>
          </div>
        `;
      renderTimeLine("NetworkTimeLine", networkTimeLineInnerHTML);
      networkBandByKey[ip + networkCard.Name] = Number.isFinite(Number(bandwidthMbps)) ? Number(bandwidthMbps) : 1;
    });
  }
  networkBandByIp[ip] = allBandwidth;

  let networkInnerHTML =
    `
      <div><strong>Agent Count</strong><br><p class="agentNum" data-ip="${ip}"></p></div>
      <div><strong>IP:</strong> ${ip}</div>

      <!-- 横スクロール可能なテーブルラッパー -->
      <div class="network-table-wrapper">
        <table class="network-table">
          <colgroup>
            <col style="width: 34%;">
            <col style="width: 33%;">
            <col style="width: 33%;">
          </colgroup>
          <thead>
            <tr>
              <th>Interface</th>
              <th>Upload</th>
              <th>Download</th>
            </tr>
          </thead>
          <tbody>
            ${networkRows}
          </tbody>
        </table>
        <div style="margin-top: 0.8em;"><strong>Network Usage</strong></div>
        <div class="chart-container">
          <canvas class="networkChart canvas" data-ip="${ip}"></canvas>
        </div>
      </div>
    `;
  renderCard("dashboard-network", networkInnerHTML);

  let netJVMTimeLine =
    `
      <div style="margin-top:0.4em;"><strong>${ip} JFR Upload Speed:</strong>
        <span class="netJFRUpNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong> JFR Upload Speed</strong></div>
      <div class="chart-container" style="margin-top:0.5em;">
        <canvas class="netJFRUpLine canvas" data-ip="${ip}" width="900" height="300"></canvas>
      </div>
                        
      <div style="margin-top:0.4em;"><strong> JFR Download Speed:</strong>
        <span class="netJFRDownNow" data-ip="${ip}">-</span>
      </div>
      <div style="margin-top:0.6em;"><strong> JFR Download Speed</strong></div>
      <div class="chart-container" style="margin-top:0.5em;">
        <canvas class="netJFRDownLine canvas" data-ip="${ip}"  width="900" height="300"></canvas>
      </div>
    `;
  renderTimeLine("NetworkTimeLine", netJVMTimeLine);
}

// --- ネットワークスピードカード ---
function renderNetworkSpeedCard(sender, ipAddresses) {
  let networkSpeedInnerHTML = ``;
  (ipAddresses || []).forEach(receiver => {
    networkSpeedInnerHTML +=
      `
        <tr>
          <td>${sender}</td>
          <td>${receiver}</td>
          <td class="uploadTd" data-sender="${sender}" data-receiver="${receiver}"></td>
          <td class="downloadTd" data-sender="${sender}" data-receiver="${receiver}"></td>
        </tr>       
        <tr>
          <td>${receiver}</td>
          <td>${sender}</td>
          <td class="uploadTd" data-sender="${receiver}" data-receiver="${sender}"></td>
          <td class="downloadTd" data-sender="${receiver}" data-receiver="${sender}"></td>
        </tr>
      `;
  });
  const networkSpeedBody = getElementConfirmlyById("networkSpeedBody");
  networkSpeedBody.innerHTML += networkSpeedInnerHTML;
}

function updateDynamicData(dynamicData) {
  const dynamicPCInfos = dynamicData.dynamicPCInfos || {};

  for (const ip in dynamicPCInfos) {
    const dynamicPCInfo = dynamicPCInfos[ip];
    const now = Date.now();

    document.querySelectorAll(`.agentNum[data-ip="${ip}"]`).forEach(el => {
      el.textContent = dynamicPCInfo.AgentsNum;
    });

    updateLoadAverageCard(ip, dynamicPCInfo.LoadAverage, now);
    updateCPUCard(ip, dynamicPCInfo.CPU, now);
    updateMemoryCard(ip, dynamicPCInfo.Memory, now);
    updateGCCard(
      ip,
      dynamicPCInfo.GCStats.Collectors["G1 Young Generation"].CollectionCount,
      dynamicPCInfo.GCStats.Collectors["G1 Old Generation"].CollectionCount,
      dynamicPCInfo.GCStats,
      now
    );
    updateGPUsCard(ip, dynamicPCInfo.GPUs, now);
    updateNetworkCardsCard(
      ip,
      dynamicPCInfo.NetworkCards,
      dynamicPCInfo.socketReadBytes,
      dynamicPCInfo.socketWriteBytes,
      now
    );
    updateNetworkSpeedsCard(dynamicPCInfo.NetworkSpeeds);
  }

  // ★ ここで DynamicPCInfo からエージェントをまとめて描画
  updateAgentsView(dynamicPCInfos);
}

function updateTextContentByQuery(query, content) {
  const container = document.querySelector(query);
  if (container) {
    container.textContent = content;
  } else {
    console.log(`couldn't find Id(${query}).`);
  }
}

function updateCanvasByQuery(query, ip, chartInstances, Percent, colorCode) {
  const container = document.querySelector(query);
  if (container) {
    if (chartInstances[ip]) {
      const chart = chartInstances[ip];
      chart.data.datasets[0].data = [Percent, 100 - Percent];
      chart.data.datasets[0].valueText = `${Percent}%`; // 中央表示テキスト
      chart.update();
    } else {
      chartInstances[ip] = new Chart(container.getContext("2d"), {
        type: 'doughnut',
        data: {
          datasets: [{
            data: [Percent, 100 - Percent],
            backgroundColor: [colorCode, '#eeeeee'],
            borderWidth: 0,
            valueText: `${Percent}%`
          }]
        },
        options: {
          animation: {
            duration: 300,
            easing: 'easeOutQuart'
          },
          cutout: "70%",
          plugins: {
            tooltip: {
              enabled: false
            },
            legend: {
              display: false
            }
          }
        },
        plugins: [centerTextPlugin]
      });
    }
  } else {
    console.log(`couldn't find Id(${query}).`);
  }
}

function updateTimeLineByQuery(query, key, now, value, valueTitle, max) {
  const lineCanvas = document.querySelector(query);

  if (lineCanvas) {
    const buf = pushPoint(key, now, value);
    const chart = ensureLineChart(lineCanvas, valueTitle);
    chart.data.datasets[0].data = buf;
    chart.options.scales.y.max = Math.max(1, Math.ceil(max));
    chart.update('none');
  }
}

function updateLoadAverageCard(ip, loadAverage, now) {
  const laForDisplay = loadAverage.toFixed(2);
  updateTextContentByQuery(`.loadAveNow[data-ip="${ip}"]`, laForDisplay);
  updateTextContentByQuery(`.LoadAverage[data-ip="${ip}"]`, laForDisplay);

  updateTimeLineByQuery(
    `.loadAveLine.canvas[data-ip="${ip}"]`,
    bufKey('loadAve', ip),
    now,
    laForDisplay,
    'LoadAve',
    30
  );
}

function updateCPUCard(ip, cpu, now) {
  const clockGHz = (cpu.ClockSpeed / 1_000_000_000).toFixed(2);
  updateTextContentByQuery(`.cpuClockSpeed[data-ip="${ip}"]`, clockGHz + "GHz");
  const cpuPercent = (cpu.LoadPercentByMXBean * 100).toFixed(2);
  updateCanvasByQuery(
    `.cpuChart.canvas[data-ip="${ip}"]`,
    ip,
    cpuChartInstances,
    cpuPercent,
    '#f44336'
  );

  const cpuPerf = cpu.LoadPercentByMXBean * (cpuScoreByIp[ip] || 1);
  updateTextContentByQuery(`.cpuPerfNow[data-ip="${ip}"]`, cpuPerf);

  updateTimeLineByQuery(
    `.cpuPerfLine.canvas[data-ip="${ip}"]`,
    bufKey('cpuPerf', ip),
    now,
    cpuPerf,
    'CPU Perf',
    60000
  );

  updateTextContentByQuery(`.cpuLoadNow[data-ip="${ip}"]`, (cpu.ProcessCpuLoad * 100).toFixed(2));
  updateTimeLineByQuery(
    `.cpuLoadLine.canvas[data-ip="${ip}"]`,
    bufKey('cpuLoad', ip),
    now,
    cpu.ProcessCpuLoad * 100,
    'CPU Load',
    100
  );

  updateTimeLineByQuery(
    `.cpuJVMSysLine.canvas[data-ip="${ip}"]`,
    bufKey('cpuJVMSys', ip),
    now,
    cpu.jvmSystem * 100,
    'CPU JVM Sys',
    100
  );

  updateTimeLineByQuery(
    `.cpuJVMUserLine.canvas[data-ip="${ip}"]`,
    bufKey('cpuJVMUser', ip),
    now,
    cpu.jvmUser * 100,
    'CPU JVM User',
    100
  );

  updateTimeLineByQuery(
    `.cpuJVMLine.canvas[data-ip="${ip}"]`,
    bufKey('cpuJVM', ip),
    now,
    cpu.total * 100,
    'CPU JVM',
    100
  );
}

function updateMemoryCard(ip, memory, now) {
  const freeGB  = (memory.HostAvailableBytes / 1024 / 1024 / 1024).toFixed(2);
  const totalGB = (memory.HostTotalBytes      / 1024 / 1024 / 1024).toFixed(2);
  const usedGB  = (totalGB - freeGB).toFixed(2);
  updateTextContentByQuery(`.usedMemory[data-ip="${ip}"]`, usedGB + "GB");

  const memoryPercent = Math.round((usedGB / totalGB) * 100);
  updateCanvasByQuery(
    `.memoryChart.canvas[data-ip="${ip}"]`,
    ip,
    memoryChartInstances,
    memoryPercent,
    '#03a9f4'
  );

  updateTextContentByQuery(`.memRealNow[data-ip="${ip}"]`, usedGB);
  updateTimeLineByQuery(
    `.memRealLine.canvas[data-ip="${ip}"]`,
    bufKey('memReal', ip),
    now,
    usedGB,
    'Mem GB',
    totalGB
  );

  const usedSwapGB  = (memory.SwapUsedBytes  / 1024 / 1024 / 1024).toFixed(2);
  const totalSwapGB = (memory.SwapTotalBytes / 1024 / 1024 / 1024).toFixed(2);
  updateTextContentByQuery(`.memSwapNow[data-ip="${ip}"]`, usedSwapGB);
  updateTimeLineByQuery(
    `.memSwapLine.canvas[data-ip="${ip}"]`,
    bufKey('memSwap', ip),
    now,
    usedSwapGB,
    'Mem GB',
    totalSwapGB
  );

  const usedHeapGB  = (memory.JvmHeapUsed      / 1024 / 1024 / 1024).toFixed(2);
  const totalHeapGB = (memory.JvmHeapCommitted / 1024 / 1024 / 1024).toFixed(2);
  updateTextContentByQuery(`.memHeapNow[data-ip="${ip}"]`, usedHeapGB);
  updateTimeLineByQuery(
    `.memHeapLine.canvas[data-ip="${ip}"]`,
    bufKey('memHeap', ip),
    now,
    usedHeapGB,
    'Mem GB',
    totalHeapGB
  );

  const usedNonHeapGB  = (memory.JvmNonHeapUsed      / 1024 / 1024 / 1024).toFixed(2);
  const totalNonHeapGB = (memory.JvmNonHeapCommitted / 1024 / 1024 / 1024).toFixed(2);
  updateTextContentByQuery(`.memNonHeapNow[data-ip="${ip}"]`, usedNonHeapGB);
  updateTimeLineByQuery(
    `.memNonHeapLine.canvas[data-ip="${ip}"]`,
    bufKey('memNonHeap', ip),
    now,
    usedNonHeapGB,
    'Mem GB',
    totalNonHeapGB
  );
}

function updateGCCard(ip, gcYoung, gcOld, gcStats, now) {
  updateTimeLineByQuery(
    `.memGCYoungLine.canvas[data-ip="${ip}"]`,
    bufKey('memGCYoung', ip),
    now,
    gcYoung,
    ' times',
    20
  );

  updateTimeLineByQuery(
    `.memGCOldLine.canvas[data-ip="${ip}"]`,
    bufKey('memGCOld', ip),
    now,
    gcOld,
    ' times',
    20
  );

  updateTimeLineByQuery(
    `.memGCJFRLine.canvas[data-ip="${ip}"]`,
    bufKey('memGCJFR', ip),
    now,
    gcStats.gcCountByJFR,
    ' times',
    20
  );

  updateTimeLineByQuery(
    `.memGCJFRPauseLine.canvas[data-ip="${ip}"]`,
    bufKey('memGCJFRPause', ip),
    now,
    gcStats.gcPauseMillis,
    ' times',
    gcStats.gcPauseMillis * 1.5
  );
}

function updateGPUsCard(ip, gpus, now) {
  if (Object.keys(gpus || {}).length > 0) {
    const gpusArr = Array.isArray(gpus)
      ? gpus
      : Object.values(gpus || []);

    gpusArr.forEach(gpu => {
      const gpuName = gpu.Name || "Unknown";

      const tempC   = gpu.TemperatureC ?? "Unknown";
      updateTextContentByQuery(
        `.gpuTemperature[data-ip="${ip}"][data-gpu="${gpuName}"]`,
        `${tempC}℃`
      );

      const usedMB  = gpu.UsedMemory ?? 0;
      const totalMB = gpu.TotalMemory ?? 0;
      updateTextContentByQuery(
        `.gpuMemory[data-ip="${ip}"][data-gpu="${gpuName}"]`,
        usedMB + `MB / ` + totalMB + `MB`
      );

      const gpuPercent = Math.max(0, Math.min(100, Number(gpu.LoadPercent ?? 0)));

      const gpuBenchmarkScore = findGPUBenchmark(gpuName);
      const gpuPerf = gpu.LoadPercent * gpuBenchmarkScore / 100;

      updateCanvasByQuery(
        `.gpuChart.canvas[data-ip="${ip}"][data-gpu="${gpuName}"]`,
        ip,
        gpuChartInstances,
        gpuPercent,
        '#4caf50'
      );

      updateTimeLineByQuery(
        `.gpuPerfLine.canvas[data-ip="${ip}"][data-gpu="${gpuName}"]`,
        bufKey('gpuPerf', ip, gpuName),
        now,
        gpuPerf,
        'Perf',
        40000
      );
      updateTimeLineByQuery(
        `.gpuTempLine.canvas[data-ip="${ip}"][data-gpu="${gpuName}"]`,
        bufKey('gpuTemp', ip, gpuName),
        now,
        tempC,
        'Temp C',
        150
      );
      updateTimeLineByQuery(
        `.gpuMemLine.canvas[data-ip="${ip}"][data-gpu="${gpuName}"]`,
        bufKey('gpuMem', ip, gpuName),
        now,
        usedMB,
        'Mem GB',
        totalMB
      );
    });
  }
}

function updateNetworkCardsCard(ip, networkCards, socketReadBytes, socketWriteBytes, now) {
  let allNetworkSpeed = 0;

  updateTimeLineByQuery(
    `.netJFRUpLine.canvas[data-ip="${ip}"]`,
    bufKey('netJFRUp', ip),
    now,
    socketReadBytes * 8 / 1_000_000,
    'Upload Mbps',
    socketReadBytes * 1.5 * 8 / 1_000_000
  );

  updateTimeLineByQuery(
    `.netJFRDownLine.canvas[data-ip="${ip}"]`,
    bufKey('netJFRDown', ip),
    now,
    socketWriteBytes * 8 / 1_000_000,
    'Download Mbps',
    socketWriteBytes * 1.5 * 8 / 1_000_000
  );

  Object.entries(networkCards).forEach(([name, networkCard]) => {
    const uploadMbps   = ((networkCard.UploadSpeed   || 0) * 8 / 1_000_000).toFixed(2);
    const downloadMbps = ((networkCard.DownloadSpeed || 0) * 8 / 1_000_000).toFixed(2);

    if (uploadMbps == 0 && downloadMbps == 0) {
      hideHTMLByQuery(`.upload[data-ip="${ip}"][data-NC="${name}"]`);
      hideHTMLByQuery(`.download[data-ip="${ip}"][data-NC="${name}"]`);
      hideHTMLByQuery(`.netUpNow[data-ip="${ip}"][data-net="${name}"]`);
      hideHTMLByQuery(`.netUpLine.canvas[data-ip="${ip}"][data-net="${name}"]`);
      hideHTMLByQuery(`.netDownNow[data-ip="${ip}"][data-net="${name}"]`);
      hideHTMLByQuery(`.netDownLine.canvas[data-ip="${ip}"][data-net="${name}"]`);
    } else {
      updateTextContentByQuery(
        `.upload[data-ip="${ip}"][data-NC="${name}"]`,
        uploadMbps + ` Mbps`
      );
      updateTextContentByQuery(
        `.download[data-ip="${ip}"][data-NC="${name}"]`,
        downloadMbps + ` Mbps`
      );

      updateTextContentByQuery(
        `.netUpNow[data-ip="${ip}"][data-net="${name}"]`,
        uploadMbps + " Mbps"
      );
      updateTimeLineByQuery(
        `.netUpLine.canvas[data-ip="${ip}"][data-net="${name}"]`,
        bufKey('netUp', ip, name),
        now,
        (networkCard.UploadSpeed || 0) * 8 / 1_000_000,
        'Upload Mbps',
        networkBandByKey[ip + name]
      );

      updateTextContentByQuery(
        `.netDownNow[data-ip="${ip}"][data-net="${name}"]`,
        downloadMbps + " Mbps"
      );
      updateTimeLineByQuery(
        `.netDownLine.canvas[data-ip="${ip}"][data-net="${name}"]`,
        bufKey('netDown', ip, name),
        now,
        (networkCard.DownloadSpeed || 0) * 8 / 1_000_000,
        'Download Mbps',
        networkBandByKey[ip + name]
      );

      allNetworkSpeed += (networkCard.UploadSpeed + networkCard.DownloadSpeed);
    }
  });

  const networkPercent = Math.round(allNetworkSpeed / networkBandByIp[ip] * 100);
  updateCanvasByQuery(
    `.networkChart.canvas[data-ip="${ip}"]`,
    ip,
    networkChartInstances,
    networkPercent,
    '#03a9f4'
  );
}

function hideHTMLByQuery(query) {
  const container = document.querySelector(query);
  if (container)
    container.style.display = "none";
}

function updateNetworkSpeedsCard(networkSpeeds) {
  const networkSpeedsArr = Array.isArray(networkSpeeds)
    ? networkSpeeds
    : Object.values(networkSpeeds || {});

  networkSpeedsArr.forEach(ns => {
    const networkUploadSpeed   = ns.UploadSpeedByOriginal.toFixed(0);
    const networkDownloadSpeed = ns.DownloadSpeedByOriginal.toFixed(0);
    updateTextContentByQuery(
      `.uploadTd[data-sender="${ns.Sender}"][data-receiver="${ns.Receiver}"]`,
      networkUploadSpeed
    );
    updateTextContentByQuery(
      `.downloadTd[data-sender="${ns.Sender}"][data-receiver="${ns.Receiver}"]`,
      networkDownloadSpeed
    );
  });
}

/* ===== Agents: DynamicPCInfo から IP ごとに表示 ===== */
let agentsIpFilterBound = false;

function updateAgentsView(dynamicPCInfos) {
  const tbody       = document.getElementById("agents-tbody");
  const ipFilter    = document.getElementById("agents-ip-filter");
  const byIpContainer = document.getElementById("agents-by-ip");

  if (!tbody || !ipFilter || !byIpContainer) {
    console.log("agents DOM not found");
    return;
  }

  // DynamicPCInfo の Map<ip, DynamicPCInfo> から
  // { ip, id, name, startTime } の配列を作る
  const flat = [];
  Object.entries(dynamicPCInfos || {}).forEach(([ip, pcInfo]) => {
    const agentsMap = pcInfo.Agents || pcInfo.agents || {};
    Object.entries(agentsMap).forEach(([key, ag]) => {
      const id   = ag.ID   ?? ag.id   ?? key;
      const name = ag.Name ?? ag.name ?? "";
      const st   = ag.StartTime ?? ag.startTime ?? null;
      flat.push({ ip, id, name, startTime: st });
    });
  });

  // 初回だけフィルタにイベント登録
  if (!agentsIpFilterBound) {
    ipFilter.addEventListener("change", () => {
      updateAgentsView(dynamicPCInfos);
    });
    agentsIpFilterBound = true;
  }

  // ===== IP セレクト更新 =====
  const ipSet = new Set(flat.map(a => a.ip));
  const prevSelected = ipFilter.value || "all";

  ipFilter.innerHTML = `<option value="all">All</option>`;
  Array.from(ipSet).sort().forEach(ip => {
    const opt = document.createElement("option");
    opt.value = ip;
    opt.textContent = ip;
    ipFilter.appendChild(opt);
  });

  if (prevSelected !== "all" && ipSet.has(prevSelected)) {
    ipFilter.value = prevSelected;
  } else {
    ipFilter.value = "all";
  }

  const activeFilter = ipFilter.value;
  const filtered = flat.filter(a =>
    activeFilter === "all" ? true : a.ip === activeFilter
  );

  // ===== 左側テーブル（IP, Agent ID, Name, StartTime） =====
  tbody.innerHTML = "";
  filtered.forEach(a => {
    const tr = document.createElement("tr");
    const startStr = a.startTime
      ? new Date(a.startTime).toLocaleString()
      : "";
    tr.innerHTML = `
      <td>${a.ip}</td>
      <td>${a.id}</td>
      <td>${a.name}</td>
      <td>${startStr}</td>
    `;
    tbody.appendChild(tr);
  });

  // ===== 右側 IP ごとのカード =====
  byIpContainer.innerHTML = "";

  const grouped = {};
  filtered.forEach(a => {
    if (!grouped[a.ip]) grouped[a.ip] = [];
    grouped[a.ip].push(a);
  });

  Object.keys(grouped).sort().forEach(ip => {
    const agents = grouped[ip];

    const rowsHtml = agents.map(a => {
      const startStr = a.startTime
        ? new Date(a.startTime).toLocaleString()
        : "";
      return `
        <tr>
          <td>${a.id}</td>
          <td>${a.name}</td>
          <td>${startStr}</td>
        </tr>
      `;
    }).join("");

    const card = document.createElement("div");
    card.className = "agent-ip-card";
    card.innerHTML = `
      <div class="agent-ip-card-header">
        <span>
          <span class="agent-ip-label">IP</span>
          <span class="agent-ip-value">${ip}</span>
        </span>
        <span class="agent-ip-count">${agents.length} agents</span>
      </div>
      <table class="agent-ip-table">
        <thead>
          <tr>
            <th>Agent ID</th>
            <th>Name</th>
            <th>StartTime</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml}
        </tbody>
      </table>
    `;
    byIpContainer.appendChild(card);
  });
}

// --- タイムライン用のチャートインスタンスとリングバッファ ---
const lineCharts = {
  loadAve: {},
  cpuPerf: {},
  cpuClock: {},
  cpuProc: {},
  cpuLoad: {},
  cpuJVMSys: {},
  cpuJVMUser: {},
  cpuTotal: {},
  memReal: {},
  memSwap: {},
  memHeap: {},
  memNonHeap: {},
  memGCYoung: {},
  memGCOld: {},
  memGCJFR: {},
  memGCJFRPause: {},
  gpuPerf: {},
  gpuTemp: {},
  gpuMem: {},
  netUp: {},
  netDown: {},
  netJFRUp: {},
  netJFRDown: {}
};
const seriesBuffers = {};

function bufKey(kind, ip, extra = "") {
  return extra ? `${kind}::${ip}::${extra}` : `${kind}::${ip}`;
}

function pushPoint(key, x, y, maxN = 120) {
  if (!seriesBuffers[key]) seriesBuffers[key] = [];
  const arr = seriesBuffers[key];
  arr.push({ x, y });
  if (arr.length > maxN) arr.shift();
  return arr;
}

function ensureLineChart(canvas, label) {
  if (canvas._chart) return canvas._chart;
  const ctx = canvas.getContext('2d');
  const chart = new Chart(ctx, {
    type: 'line',
    data: { datasets: [{ label, data: [], borderWidth: 2, tension: 0.2, pointRadius: 0 }] },
    options: {
      animation: false,
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { type: 'time', time: { unit: 'second' } },
        y: { beginAtZero: true }
      },
      plugins: { legend: { display: false } }
    }
  });
  canvas._chart = chart;
  return chart;
}

const cpuScoreByIp = {};
const gpuScoreByKey = {};
const networkBandByKey = {};
const networkBandByIp = {};

function applySchedulerConfig() {
  const strategy = document.getElementById("strategy-select").value;
  const interval = Number.parseInt(document.getElementById("interval-input").value, 10);
  const agentObserveTime = Number(document.getElementById("observe-input").value);
  const remigrateProhibitTime = Number(document.getElementById("remigrate-input").value);
  const agentEMAAlpha = Number.parseFloat(document.getElementById("ema-input").value);

  const payload = {
    strategy,
    interval,
    agentObserveTime,
    remigrateProhibitTime,
    agentEMAAlpha
  };

  if (!strategy) { alert("strategy を選んでください"); return; }
  if (!Number.isFinite(interval) || interval <= 0) { alert("interval が不正"); return; }
  if (!Number.isFinite(agentObserveTime) || agentObserveTime < 0) { alert("observe が不正"); return; }
  if (!Number.isFinite(remigrateProhibitTime) || remigrateProhibitTime < 0) { alert("remigrate が不正"); return; }
  if (!Number.isFinite(agentEMAAlpha) || agentEMAAlpha < 0 || agentEMAAlpha > 1) { alert("ema が不正(0〜1)"); return; }

  fetch('/api/scheduler/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then(async (res) => {
    if (res.ok) {
      alert("Scheduler config applied!");
    } else {
      const msg = await res.text().catch(() => "");
      alert("Failed to apply config: " + msg);
    }
  }).catch(err => {
    alert("Network error: " + err.message);
  });
}

// タブ切り替え
function showTab(tabId) {
  document.querySelectorAll(".tab").forEach(btn => btn.classList.remove("active"));
  document.querySelectorAll(".tab-content").forEach(div => div.classList.remove("active"));
  document.querySelector(`.tab[onclick="showTab('${tabId}')"]`).classList.add("active");
  document.getElementById(tabId).classList.add("active");
}

const cpuChartInstances = {};
const memoryChartInstances = {};
const gpuChartInstances = {};
const networkChartInstances = {};

// グラフの真ん中にテキストを表示するプラグイン
const centerTextPlugin = {
  id: 'centerText',
  beforeDraw(chart) {
    const { width, height, ctx } = chart;
    const dataset = chart.config.data.datasets[0];
    const text = dataset.valueText || '';

    ctx.save();
    ctx.font = 'bold 16px sans-serif';
    ctx.fillStyle = '#333';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, width / 2, height / 2);
    ctx.restore();
  }
};

let cpuBenchmarks = {};
let gpuBenchmarks = {};

async function loadBenchmarks() {
  const res = await fetch("cpu_benchmarks.json");
  cpuBenchmarks = await res.json();
  const res2 = await fetch("gpu_benchmarks.json");
  gpuBenchmarks = await res2.json();
}

function findCPUBenchmark(cpuName) {
  if (!cpuName || Object.keys(cpuBenchmarks).length === 0) return "Unknown";
  const normalized = cpuName.toLowerCase();

  for (const key in cpuBenchmarks) {
    const keyNormalized = key.toLowerCase();
    if (normalized.includes(keyNormalized) || keyNormalized.includes(normalized)) {
      return cpuBenchmarks[key];
    }
  }

  return "Unknown";
}

function findGPUBenchmark(gpuName) {
  if (!gpuName || Object.keys(gpuBenchmarks).length === 0) return "Unknown";
  const normalized = gpuName.toLowerCase();

  for (const key in gpuBenchmarks) {
    const keyNormalized = key.toLowerCase();
    if (normalized.includes(keyNormalized) || keyNormalized.includes(normalized)) {
      return gpuBenchmarks[key];
    }
  }

  return "Unknown";
}

async function main() {
  await loadBenchmarks();
}

main();